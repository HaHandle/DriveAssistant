# UI 데이터 기반과 GitHub 확인 결과

## 이번 단계

프론트엔드 계획의 1~2단계인 UI 전용 모델과 Mock 공급원을 구현했습니다. 정상·로딩·빈 기록·오류·부분 누락·티어 구성 예시·최고 티어 구성 예시를 제공합니다. 기존 앱 화면과 차량 UX 제한은 그대로 유지합니다.

서버 계약, 인증 방식, 엔드포인트, JSON 필드, 실제 점수·티어 산정 규칙은 정의하지 않았습니다. 다음 단계에서 ViewModel이 저장소를 구독하고 기존 화면으로 상태를 전달합니다.

## GitHub에서 확인한 기존 구현

확인 날짜: 2026-11-10. 연결된 두 GitHub 계정에서 접근 가능한 저장소 목록과 `HaHandle/DriveAssistant`의 브랜치 목록, 주요 기능 브랜치의 파일 트리를 조회했습니다. 접근 범위 밖의 저장소 존재 여부는 확인할 수 없습니다.

별도 서버 저장소나 OpenAPI 명세는 확인되지 않았습니다. 대신 같은 저장소의 기능 브랜치에 기존 분석·연수 코드가 있습니다.

| 위치 | 확인한 내용 | 이번 단계에서의 처리 |
| --- | --- | --- |
| `feat/drive-coach-mvp` | 정차 분석, 차로 변경 평가, 다음 연습 추천, 연수 기록 | 원본 코드는 변경하거나 병합하지 않음 |
| `feat/deceleration-event-analysis` | 속도·감속 사건·정차 관찰 | 향후 UI 매핑 근거로 기록 |
| `feat/turn-signal-practice` | 신호 시작·유지·소등 및 간격 판단 | 평가 항목을 서로 구분 |
| `feat/beginner-driver-training` | 계층별 책임과 의존성 방향을 설명한 문서 | UI 모델에 차량 SDK·직렬화 의존성을 추가하지 않음 |

확인에 사용한 주요 원본:

- [통합 MVP README](https://github.com/HaHandle/DriveAssistant/blob/3ddd6f77e08db498309e0f7007ec1c7551bdf70e/README.md)
- [PracticeRecord.kt](https://github.com/HaHandle/DriveAssistant/blob/3ddd6f77e08db498309e0f7007ec1c7551bdf70e/app/src/main/java/com/example/drivingcoach/PracticeRecord.kt)
- [DecelerationSession.kt](https://github.com/HaHandle/DriveAssistant/blob/3ddd6f77e08db498309e0f7007ec1c7551bdf70e/app/src/main/java/com/example/drivingcoach/DecelerationSession.kt)
- [통합 및 검증 범위](https://github.com/HaHandle/DriveAssistant/blob/3ddd6f77e08db498309e0f7007ec1c7551bdf70e/docs/release-readiness.md)
- [기존 아키텍처 문서](https://github.com/HaHandle/DriveAssistant/blob/ed9c1c9b86804ea0b85a81ec4eee1175d111b60f/docs/ARCHITECTURE.md)

`PracticeRecord`는 `met`/`observed` 횟수와 다음 연습 항목을 저장합니다. 통합 문서는 정차와 차로 변경을 하나의 운전자 점수로 합산하지 않는다고 설명합니다. `DecelerationResult.score`는 nullable이며 선택적인 감속 데모 점수입니다. 이 값을 종합 운전 점수나 티어로 재해석하지 않습니다. 기존 저장 방식도 네트워크 서버가 아닌 기기 내 연수 기록입니다.

## UI 전용 모델

| 모델 | 의미와 경계 |
| --- | --- |
| `DriverOverview` | 표시용 묶음. Mock/기록 출처를 명시 |
| `DrivingScore` | 현재 화면의 0~100 표시 범위. 서버 산정 규칙을 의미하지 않음 |
| `DrivingScoreSummary` | 현재·이전 표시값과 추이. 미측정은 null이며 비교값도 계산하지 않음 |
| `DrivingHabitAnalysis` | 정차·신호 시작·유지·소등을 독립적으로 표시 |
| `ObservationCount` | 관찰 횟수와 충족 횟수. 0회 관찰과 관찰 후 미충족을 구분 |
| `DriverProgress.Unassigned` | 기본 상태. 숙련도·티어 기준 미정 |
| `DriverProgress.Preview` | 배치 검토용 레벨·라벨·경험치. 기록 데이터와 결합할 수 없음 |

경험치의 예시 진행률은 해당 예시 티어 안의 값만 사용합니다. 레벨·라벨 A/Z·목표 수치는 제품 정책이 아닙니다. 최고 티어 예시는 다음 목표를 null로 두어 일반적인 0/0 진행률과 구분합니다.

## 데이터 흐름

`DriverOverviewRepository.observeOverview()`는 `Flow<DriverOverviewState>` 경계를 제공합니다. 실제 통신 구현은 없습니다. Mock 구현은 수집할 때마다 동일한 상태 한 개를 내보내며 지연·자동 상태 전환·센서·저장·네트워크 호출을 하지 않습니다. LOADING 시나리오는 로딩 화면 검토용으로 그 상태에 머뭅니다.

Preview는 `DriverOverviewPreviewParameterProvider`로 같은 fixture를 직접 주입하므로 Activity, ViewModel 또는 차량 서비스가 필요하지 않습니다. 현재 앱의 `DrivingCoachApp`에는 이 저장소를 아직 연결하지 않았습니다. 따라서 차량에서 Mock 값이 자동 노출되는 경로도 추가하지 않았습니다.

기존 코루틴 해결 버전인 1.9.0의 `core` 의존성을 명시했습니다. 다른 의존성은 업그레이드하지 않았습니다.

## 검증과 팀원 확인

- 점수 미측정/0점, 비교 방향, 관찰 부족, 티어 미정, 경험치 경계값을 JVM 단위 테스트로 검증합니다.
- 임시 티어가 기록 출처의 데이터로 생성되는 경우를 모델에서 거부합니다.
- Mock 시나리오의 반복 수집 결과가 같은지 검사합니다.
- `assembleDebug`, `testDebugUnitTest`, `lintDebug`, `assembleDebugAndroidTest`로 컴파일·회귀를 확인합니다.
- 에뮬레이터는 사용하지 않습니다. Preview 코드는 빌드로 컴파일 검증하며 실제 시각 검토는 Android Studio에서 수행해야 합니다.

이번 변경 검증 결과: 디버그 APK 빌드, JVM 단위 테스트 19개(이번 단계 추가 13개), Android Lint, UI 테스트 APK 컴파일이 통과했습니다. 기기 UI 테스트와 Android Studio Preview의 실제 렌더링은 실행 확인하지 않았습니다.

Android Studio에서 `ui/preview/DriverOverviewPreview.kt`의 Split/Design 보기를 엽니다. 각 테마의 Preview 매개변수는 `OverviewScenario` 순서대로 정상, 로딩, 빈 기록, 오류, 부분 누락, 티어 예시, 최고 티어 예시입니다.

부분 누락 시 미측정이 0점으로 보이지 않아야 합니다. 기본 정상 화면의 티어는 미정이어야 하며, 두 티어 예시에는 실제 등급이 아니라는 문구가 있어야 합니다. 관찰 부족에서는 0/0 충족 비율을 표시하지 않습니다.

## 다음 단계

ViewModel/상태 수집 계층을 추가한 뒤 기존 앱 화면을 새 모델에 연결합니다. SharedPreferences 키, 탐색 탭과 차량 UX 제한을 보존하고 Preview에는 상태를 직접 전달합니다. 이후 대시보드, 상세 분석, 숙련도·티어 화면을 순서대로 구현합니다.
