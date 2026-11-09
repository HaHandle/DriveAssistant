# PR #7 Pleos emulator validation for c3737c5

## Target

- Branch: `review/pr7-visual`
- Commit: `c3737c5aa63010da0cc57b5733ca8a6801da0204`
- Package: `com.example.drivingcoach`
- Emulator: Pleos Connect, `Connect on x86_64 emulator`
- Android: 14 / API 34
- Evidence log: `pr7-c3737c5-android-logcat.txt`

## Confirmed

- The PR #7 APK replaced the previously installed package at `2026-11-09 06:46:58`.
- `MainActivity` launch returned result code `0`.
- The app process started as PID `18568`.
- `MainActivity` remains resumed and focused in the Pleos multi-window environment.
- `android.car.permission.CAR_SPEED` is granted for the active app user, User 10.
- No matching `FATAL EXCEPTION`, `Process: com.example.drivingcoach`, or `AndroidRuntime` crash entry was found in the retained Logcat buffer.

## Not confirmed

- Actual vehicle speed samples reaching the application.
- Driving-restriction callback values reaching the application.
- End-to-end training flow after speed data becomes available.

The application currently does not write speed-sample or driving-restriction callback values to Logcat, so those behaviors cannot be established from this log alone.

Screenshots and video evidence will be added separately.
