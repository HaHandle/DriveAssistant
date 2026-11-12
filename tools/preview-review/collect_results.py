"""Inventory actual renderer outputs; do not treat new reference images as reviewed baselines."""
import base64
import hashlib
import json
import pathlib
import re
import struct
import subprocess
import xml.etree.ElementTree as ET

root = pathlib.Path(__file__).resolve().parent
references = root / "src/screenshotTestDebug/reference"
images = sorted(references.rglob("*.png")) if references.exists() else []
suites = [ET.parse(p).getroot() for p in (root / "build/test-results/updateDebugScreenshotTest").glob("TEST-*.xml")]
manifest = {
    "commit": subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip(),
    "tests": sum(int(s.get("tests", 0)) for s in suites),
    "failures": sum(int(s.get("failures", 0)) for s in suites),
    "errors": sum(int(s.get("errors", 0)) for s in suites),
    "images": [],
    "reviewed": False,
}
expected = {family: count for families, count in [
    (["HabitDay", "HabitNight", "HabitLargeText", "HabitEnglish"], 9),
    (["ScoreDay", "ScoreNight", "ScoreLargeText", "ScoreEnglish"], 8),
    (["AppDay", "AppNight", "AppLargeText", "AppEnglish"], 7),
] for family in families}
manifest["coverage"] = {family: sum(family in p.as_posix() for p in images) for family in expected}
for path in images:
    data = path.read_bytes()
    width, height = struct.unpack(">II", data[16:24])
    manifest["images"].append({"path": path.relative_to(references).as_posix(), "width": width,
                               "height": height, "sha256": hashlib.sha256(data).hexdigest()})
(root / "build").mkdir(exist_ok=True)
(root / "build/preview-manifest.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")
print("PREVIEW_MANIFEST " + json.dumps(manifest))

# Small public Mock UI samples remain readable through the connected GitHub log API.
# Full-resolution outputs for every case are also retained as the workflow artifact.
budget = 750_000
sample_ordinals = {"HabitLargeText": 8, "ScoreLargeText": 3, "AppNight": 4, "HabitDay": 6,
                   "ScoreDay": 7, "AppEnglish": 2, "HabitNight": 4, "ScoreNight": 6,
                   "AppLargeText": 3, "HabitEnglish": 8, "ScoreEnglish": 3, "AppDay": 1}
def ordinal(path):
    match = re.search(r"_(\d+)\.png$", path.name)
    return (0, int(match.group(1))) if match else (1, path.as_posix())

for family in ["HabitLargeText", "ScoreLargeText", "AppNight", "HabitDay", "ScoreDay", "AppEnglish",
               "HabitNight", "ScoreNight", "AppLargeText", "HabitEnglish", "ScoreEnglish", "AppDay"]:
    candidates = [p for p in images if family in p.as_posix()]
    if not candidates:
        continue
    candidates.sort(key=ordinal)
    path = candidates[min(sample_ordinals[family], len(candidates) - 1)]
    data = path.read_bytes()
    encoded = base64.b64encode(data).decode("ascii")
    if len(encoded) > budget:
        continue
    budget -= len(encoded)
    width, height = struct.unpack(">II", data[16:24])
    info = {"family": family, "path": path.relative_to(references).as_posix(), "width": width,
            "height": height, "sha256": hashlib.sha256(data).hexdigest()}
    print("PREVIEW_PNG_BEGIN " + json.dumps(info))
    for start in range(0, len(encoded), 120):
        print("PREVIEW_PNG_DATA " + encoded[start:start + 120])
    print("PREVIEW_PNG_END")

if (not suites or manifest["failures"] or manifest["errors"] or manifest["coverage"] != expected):
    raise SystemExit("Preview rendering is incomplete; inspect the artifact report.")
