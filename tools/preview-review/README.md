# Compose Preview review

This isolated host-side renderer compiles the actual app sources, resources, theme and existing Mock Preview providers. It does not change the production module, SDK versions or backend interfaces, and does not boot an emulator.

Run from the repository root using the existing JDK 25 / Android SDK setup:

```text
./gradlew --no-daemon -p tools/preview-review updateDebugScreenshotTest
python3 tools/preview-review/collect_results.py
```

The workflow uploads PNG files, the render report and a SHA-256 inventory. The named entry points reproduce the app's day/night, narrow 200% text, and English Preview configurations for dashboard (8 cases), habit detail (9 cases), and app states (7 cases), totaling 96 images. Collection fails if any family is missing an expected case. Generating reference images proves rendering, not visual approval; a reviewer must inspect the images before accepting a baseline.

See the [2026-11-12 render evidence and visual review](../../docs/preview-evidence/2026-11-12/README.md) for the successful 96-image CI run, five inspected samples, the large-text back-label fix, and remaining device checks.

The standalone screenshot plugin is pinned to `0.0.1-alpha16` for the project's current AGP 9.4.1. The [official guidance](https://developer.android.com/studio/preview/compose-screenshot-testing) now recommends AGP test suites on 9.5+, so keep this compatibility harness isolated until a separately reviewed toolchain upgrade. Production dependencies use the existing version catalog.

On this Windows host, application control blocked Android CLI's build helper and Layoutlib's native runtime. No policy settings were changed. The Ubuntu workflow is the rendering target; local native-library initialization failures are not app test failures or successful screenshot results.
