# PR #12 상태 연결 검증

## 대상

- PR: #12 `feat/overview-state-flow`
- 검증 HEAD: `9681181320497603c992029cd826b0eda57e6961`
- 앱 코드 기준: `0ea9b338a92714a2140a5995fac65d87445679fa`
  - 이후 `9681181`은 검증 개수 문서만 정정하며 앱 코드는 변경하지 않습니다.
- 검증일: 2026-11-12
- 환경: Pleos Connect v2.1.0 에뮬레이터, Android 14 / API 34, Android Studio Compose Preview

## 수동 실행 결과

| 증빙 | 확인 결과 |
| --- | --- |
| `screenshots/01_automotive_empty.png` | 자동차 환경에서 Mock 점수·예시 기록을 표시하지 않고 차량 데이터 연결 대기 상태를 표시합니다. |
| `screenshots/02_history_empty.png` | 주행 기록 탭에서 Mock 기록 대신 빈 기록 안내를 표시합니다. |
| `screenshots/03_before_settings_persisted.png` | 재실행 전 어두운 테마 선택 상태를 확인했습니다. |
| `screenshots/04_settings_persisted.png` | 앱 재실행 뒤 어두운 테마가 유지됐습니다. |
| `screenshots/05_driving_restricted.png` | D단·57 km/h에서 Pleos 플랫폼의 `주행 중에는 사용할 수 없습니다.` 차단 화면이 표시됐습니다. |
| `screenshots/06_park_recovered.png` | P단·0 km/h 복귀 후 앱이 다시 표시되고 빈 기록과 어두운 테마가 유지됐습니다. |

## Compose Preview 결과

`DriverOverviewPreview.kt`의 `DriverOverviewAppPreview`에 상태를 직접 주입해 확인했습니다.

| 증빙 | Preview | 확인 결과 |
| --- | --- | --- |
| `screenshots/07_preview_day_normal.png` | `App states · day - state 0` | 정상 Mock의 92점·비교값·코칭을 표시합니다. |
| `screenshots/08_preview_day_loading.png` | `App states · day - state 1` | 로딩 문구를 표시하며 이전 점수를 남기지 않습니다. |
| `screenshots/09_preview_day_empty.png` | `App states · day - state 2` | 빈 기록 문구를 표시하며 이전 점수를 남기지 않습니다. |
| `screenshots/10_preview_day_error.png` | `App states · day - state 3` | 오류 문구와 `다시 불러오기` 버튼을 표시하며 이전 점수를 남기지 않습니다. |
| `screenshots/11_preview_day_partial.png` | `App states · day - state 4` | 누락 점수를 0점으로 대체하지 않고 `미측정`으로 표시합니다. |
| `screenshots/12_preview_night_normal.png` | `App states · night - state 0` | 야간 테마에서 정상 상태의 주요 문구와 점수를 확인했습니다. |
| `screenshots/13_preview_english_wide.png` | `App states · wide English - state 0` | 넓은 영문 화면에서 점수와 습관 패널의 2열 배치를 확인했습니다. |
| `screenshots/14_preview_font_200.png` | `App states · large text - state 0` | 글자 크기 200%에서 주요 문구가 줄바꿈되고 세로 스크롤 구조와 하단 탐색이 유지됩니다. |

## 자동 검증

- 로컬 `:app:assembleDebug :app:testDebugUnitTest`: 성공
  - JVM 테스트 41개, 실패·오류·건너뜀 0
- 로컬 `:app:lintDebug :app:assembleDebugAndroidTest`: 성공
- GitHub Android CI Run #36: 성공
- ViewModel JVM 테스트 8개에서 재시도, 이전 수집 취소, 늦은 값 무시, 구독 공유·종료·재구독을 확인했습니다.
- 공급원 선택 JVM 테스트 2개에서 자동차 환경이 Mock 저장소를 선택하지 않음을 확인했습니다.
- 상태 제거·재시도·미측정·차량 Mock 차단 Compose UI 회귀 테스트는 테스트 APK 컴파일까지 확인했습니다.

## 미실시와 제한

- `connectedDebugAndroidTest`는 이번 증빙 수집에서 실행하지 않았습니다.
- TalkBack과 실제 차량 검증은 실시하지 않았습니다.
- 오류 화면의 재시도 버튼 실제 터치는 자동 UI 테스트 실행이 아니라 코드·JVM 검토와 UI 테스트 APK 컴파일로 확인했습니다.
- D단 차단 화면은 Pleos 플랫폼의 주행 제한 결과이며 앱의 개별 상세 화면을 주행 중에 노출한 결과가 아닙니다.
