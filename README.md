# DriveAssistant

플레오스 커넥트의 사용성·안전성·즉시성 원칙에 맞춘 Android Jetpack Compose 프론트엔드입니다.

- 코칭 모드 선택, 코칭 미리보기 시작·종료, 예시 주행 기록과 상세 화면
- Asta Sans, 주·야간 테마, 큰 터치 영역, 가로·세로 및 분할 창 대응
- 입력에 반응하는 짧은 리퀴드 선택 모션과 버튼 피드백
- AAOS `CarUxRestrictionsManager`를 통한 상세 화면·모션 제한

휴대전화에서는 **미리보기·예시 데이터**로 표시됩니다. 차량에서는 예시 데이터를 사용하지 않으며, 주행 데이터 공급원이 아직 연결되지 않아 대기 화면을 표시합니다. 현재 열린 브랜치에는 백엔드 클라이언트가 연결되어 있지 않습니다.

UI 전용 데이터 모델과 교체 가능한 Mock 공급원을 추가했습니다. [데이터 기반 구현과 GitHub 확인 결과](docs/frontend-data-foundation.md)에 기존 기능 브랜치의 분석 코드, 미확정 항목, 후속 연결 범위를 정리했습니다. 서버 계약과 실제 티어 기준은 확정하지 않았습니다.

팀원 검토 방법과 검증 범위는 [프론트엔드 검토 문서](docs/frontend-review.md)를 확인하세요. Android Studio에서는 `DrivingCoachApp.kt`의 Compose Preview로 넓은 창·분할 창·야간·영문·주행 제한 화면을 볼 수 있습니다.

새 데이터 시나리오는 `ui/preview/DriverOverviewPreview.kt`에서 확인합니다. 기존 앱 화면으로의 ViewModel 연결은 다음 단계이며, 이번 변경은 기존 탐색·설정·차량 제한 동작을 바꾸지 않습니다.

## 빌드

기존 프로젝트의 JDK 25, Android SDK 37.0 환경을 사용합니다.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
```

Windows 한글 경로에서 경로 검사 또는 테스트 실행기 인코딩 오류가 발생하면 다음처럼 실행합니다. 아래 옵션은 해당 실행에만 적용됩니다.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest '-Pandroid.overridePathCheck=true' '-Dorg.gradle.jvmargs=-Xmx2048m -Dfile.encoding=MS949'
```

디버그 APK: `app/build/outputs/apk/debug/app-debug.apk`

Asta Sans는 [Google Fonts 공식 원본](https://github.com/google/fonts/tree/main/ofl/astasans)을 사용하며, [SIL Open Font License](app/src/main/assets/licenses/AstaSans-OFL.txt)를 APK에도 포함합니다.
