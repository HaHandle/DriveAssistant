"""Install the official Pleos image when command-line sdkmanager omits custom sites."""
import hashlib
import os
from pathlib import Path, PurePosixPath
import shutil
import tempfile
import urllib.request
import xml.etree.ElementTree as ET
import zipfile

SITE = "https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml"
PACKAGE = "system-images;android-34;pleos_car_emulator;x86_64"
SDK = Path(os.environ["ANDROID_SDK_ROOT"])
# The archive source.properties declares android-automotive as its local tag.
LOCAL_PACKAGE = "system-images;android-34;android-automotive;x86_64"
destination = SDK.joinpath(*LOCAL_PACKAGE.split(";"))
metadata = ET.fromstring(urllib.request.urlopen(SITE, timeout=60).read())
package = next(item for item in metadata if item.tag == "remotePackage" and item.get("path") == PACKAGE)
archive = package.find("archives/archive/complete")
url = archive.findtext("url")
expected_size = int(archive.findtext("size"))
expected_checksum = archive.findtext("checksum").strip()
checksum_type = archive.find("checksum").get("type", "sha1")
if checksum_type not in {"sha1", "sha256"}:
    raise RuntimeError("Unsupported image checksum type")

with tempfile.TemporaryDirectory(prefix="pleos-image-", dir=os.environ.get("RUNNER_TEMP")) as temporary:
    archive_path = Path(temporary) / "image.zip"
    digest = hashlib.new(checksum_type)
    print("Downloading official Pleos image", expected_size, "bytes", flush=True)
    with urllib.request.urlopen(url, timeout=120) as response, archive_path.open("wb") as output:
        while chunk := response.read(8 * 1024 * 1024):
            output.write(chunk)
            digest.update(chunk)
    if archive_path.stat().st_size != expected_size or digest.hexdigest() != expected_checksum:
        raise RuntimeError("Official image checksum or size mismatch")

    with zipfile.ZipFile(archive_path) as image:
        required_space = sum(entry.file_size for entry in image.infolist())
        free_space = shutil.disk_usage(SDK).free
        print("Image extracted bytes", required_space, "available bytes", free_space, flush=True)
        if free_space < required_space + 1024 ** 3:
            raise RuntimeError("Not enough disk space for the Pleos image")
        destination.mkdir(parents=True, exist_ok=True)
        for entry in image.infolist():
            path = PurePosixPath(entry.filename)
            if path.parts[0] != "crp_avd_out_x86_64" or ".." in path.parts:
                raise RuntimeError("Unexpected image archive path")
            relative = path.parts[1:]
            target = destination.joinpath(*relative)
            if entry.is_dir():
                target.mkdir(parents=True, exist_ok=True)
            else:
                target.parent.mkdir(parents=True, exist_ok=True)
                with image.open(entry) as source, target.open("wb") as output:
                    shutil.copyfileobj(source, output, 8 * 1024 * 1024)

# Let Android tools register the archive's own source.properties metadata.
print("Installed", LOCAL_PACKAGE, flush=True)
