# 3단계: 기존 화면의 상태 연결

## 범위와 데이터 흐름

1~2단계의 UI 전용 모델과 Mock 공급원을 기존 코칭 화면에 연결합니다. 대시보드 그래프·기간 선택, 분석 상세 탐색, 숙련도·티어 전용 화면은 후속 단계입니다. 서버 API, 인증, 점수 합산, 실제 티어 기준은 정의하지 않습니다.

`MainActivity → 기기별 Repository 선택 → DriverOverviewViewModel.uiState → collectAsStateWithLifecycle → DrivingCoachApp → CoachHomeScreen`

- 일반 Android 기기는 기존 정상 Mock 시나리오를 사용합니다.
- 자동차 기기는 연결 대기 공급원을 사용하며 `Empty`를 반환합니다. 자동차의 UX 제한 변경으로 공급원을 Mock으로 바꾸지 않습니다.
- 기존 `PresentationPolicy`를 화면의 최종 제한으로 유지합니다. 차량 화면은 주입된 Mock 상태가 있더라도 예시 점수·습관을 표시하지 않습니다.
- 설정의 SharedPreferences 파일·키, 탐색 탭, 기록 예시, 코칭 미리보기 동작은 유지합니다. 미리보기는 실제 주행을 기록하지 않습니다.

## 상태와 생명주기

| 상황 | 동작 |
| --- | --- |
| 최초 구독·재시도 | `Loading`. 직전 점수를 성공 화면으로 남기지 않음 |
| 저장소의 값 | `Loading / Empty / Ready / Error`를 그대로 전달 |
| 값 없이 정상 종료 | `Empty`로 표시 |
| 저장소 획득·수집 실패 | 일반적인 `UNAVAILABLE` 오류와 재시도 버튼 표시 |
| 재시도 | 이전 수집을 취소하고 새로 수집. 이전 공급원의 늦은 값은 전달하지 않음 |
| 여러 UI 구독자 | 저장소 수집 하나를 공유 |
| 마지막 구독 종료 | 즉시 수집 취소, 캐시를 `Loading`으로 초기화 |
| ViewModel 해제 | 수집 취소. 코루틴 취소를 오류로 바꾸지 않음 |

`ViewModelProvider`가 Activity 구성 변경 중 ViewModel을 보존합니다. Compose는 `STARTED` 이상에서 상태를 수집합니다. Preview는 ViewModel을 만들지 않고 같은 상태를 직접 주입합니다. 라이프사이클 의존성은 기존 해결 버전 2.11.0을 명시하고, 테스트에는 기존 코루틴 버전 1.9.0을 사용합니다.

## 화면 연결

홈의 고정 92점 대신 전달받은 `score.current`와 `score.change`를 표시합니다. 미측정은 0점으로 바꾸지 않습니다. 기존의 고정된 긍정 평가 요약 대신 모델의 정차·신호 시작·유지·소등 결과와 관찰 횟수를 표시합니다. 같은 습관 패널을 데이터 Preview에서도 재사용합니다. 로딩·빈 기록·오류에는 예시 점수나 최근 주행 카드를 표시하지 않습니다.

로딩에는 정적인 문구를 사용합니다. 기존 입력 반응 모션·모션 끄기·차량 UX 제한은 그대로 따릅니다. 숙련도·티어 예시는 기존 데이터 Preview에만 표시하며 실제 등급을 부여하지 않습니다.

## 완료 기준과 검증

- ViewModel의 구독 시작, 로딩, 빈 응답, 부분 누락, 실패, 재시도, 이전 수집 취소, 구독 공유·종료, 해제를 JVM 테스트로 확인합니다.
- 기기별 공급원 선택에서 자동차에는 Mock이 전달되지 않는지 JVM 테스트로 확인합니다.
- 상태별 점수 제거·재시도 콜백·미측정·차량 Mock 차단의 Compose UI 회귀 테스트를 추가합니다. 에뮬레이터 없이 테스트 APK 컴파일까지만 확인합니다.
- `assembleDebug`, `testDebugUnitTest`, `lintDebug`, `assembleDebugAndroidTest`를 실행합니다.
- Android Studio의 `DriverOverviewPreview.kt`에서 `App states` 그룹을 엽니다. 7개 상태 각각의 주·야간·영문·글자 크기 200% 화면을 검토합니다.

검증 결과: `assembleDebug`, `testDebugUnitTest`, `lintDebug`, `assembleDebugAndroidTest` 모두 통과했습니다. 선행 #5 병합을 반영한 현재 JVM 테스트는 총 41개이며 실패·오류 0개입니다. 이번 단계에서 ViewModel 8개와 공급원 선택 2개를 추가했습니다. Compose UI 회귀 테스트 3개는 추가 후 컴파일했으며 실행하지 않았습니다.

Preview 실제 렌더링·스크린샷, 기기 UI 테스트, 차량 서비스 전환은 팀원의 대상 환경 확인이 필요합니다.

## 팀원 리뷰 요청

- 상태 연결과 실제 대시보드 개발의 범위가 분리되어 있는지 검토해 주세요.
- UI가 성공·오류·미측정을 혼동하지 않는지, 재시도 후 이전 값이 남지 않는지 확인해 주세요.
- 기존 분석 브랜치의 `met/observed`를 그대로 유지하고 종합 점수로 환산하지 않는 경계를 확인해 주세요.
- 차량의 연결 대기·상태 미확인·주행 제한, 테마·모션 설정과 기존 기록 탐색이 유지되는지 확인해 주세요.
- `App states` Preview의 큰 글자·분할 창에서 문구와 재시도 버튼에 접근할 수 있는지 확인해 주세요.

설계 참고: [Android StateFlow 가이드](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow), [Compose 상태 수집](https://developer.android.com/develop/ui/compose/state), [Pleos Animation](https://document.pleos.ai/docs/connect/guide/docs-design/foundations/animation).
