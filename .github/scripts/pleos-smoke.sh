#!/usr/bin/env bash
set -euo pipefail

IMAGE_PACKAGE="system-images;android-34;android-automotive;x86_64"
mkdir -p pleos-evidence
sdkmanager --channel=3 'platforms;android-37.0' 'platforms;android-34' 'build-tools;36.0.0' emulator
sdkmanager --list > pleos-evidence/sdk-packages.txt
python3 .github/scripts/install-pleos-image.py
printf 'no\n' | avdmanager create avd --force --name pleos-ci --package "$IMAGE_PACKAGE"
cat >> "$HOME/.android/avd/pleos-ci.avd/config.ini" <<'CONFIG'
hw.ramSize=4096
hw.lcd.width=2560
hw.lcd.height=1440
hw.lcd.density=160
hw.gpu.enabled=yes
hw.gpu.mode=swiftshader_indirect
fastboot.forceColdBoot=yes
fastboot.forceFastBoot=no
CONFIG

emulator -avd pleos-ci -no-window -no-snapshot -no-audio -no-boot-anim   -gpu swiftshader_indirect -memory 4096 > pleos-evidence/emulator.log 2>&1 &
emulator_pid=$!
cleanup() {
  adb logcat -d > pleos-evidence/logcat.txt 2>&1 || true
  adb shell cmd hmg_car_service --service drivingstate driving_regulation off || true
  kill "$emulator_pid" 2>/dev/null || true
}
trap cleanup EXIT
timeout 120 adb wait-for-device
booted=false
for attempt in $(seq 1 120); do
  if [[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == 1 ]]; then
    booted=true
    break
  fi
  if ! kill -0 "$emulator_pid" 2>/dev/null; then
    tail -100 pleos-evidence/emulator.log
    exit 1
  fi
  sleep 5
done
[[ "$booted" == true ]]
adb shell getprop > pleos-evidence/device-properties.txt
adb shell pm list features > pleos-evidence/device-features.txt
grep -q android.hardware.type.automotive pleos-evidence/device-features.txt
adb shell service list > pleos-evidence/device-services.txt
grep -q hmg_car_service pleos-evidence/device-services.txt
adb shell input keyevent 82
adb shell cmd hmg_car_service --service drivingstate driving_regulation off
./gradlew --no-daemon :app:connectedDebugAndroidTest   -Pandroid.testInstrumentationRunnerArguments.class=com.example.drivingcoach.PleosConnectionTest

adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell pm grant com.example.drivingcoach android.car.permission.CAR_SPEED
adb shell am start -W -n com.example.drivingcoach/.MainActivity > pleos-evidence/app-launch.txt
sleep 5
adb exec-out screencap -p > pleos-evidence/pleos-app.png
