# PR #11 Pleos 에뮬레이터 검증 결과

## 1. 수동 확인

### 확인 완료

- PR #11의 앱이 Pleos Connect 에뮬레이터에서 설치되고 실행되었습니다.
- 코칭 화면에서 `균형`, `부드러움`, `효율` 선택에 따라 선택 표시와 안내 문구가 전환됩니다.
- 차량 환경에서는 Mock 점수와 샘플 주행 기록이 표시되지 않으며, 주행 기록은 빈 상태로 표시됩니다.
- `자동`, `밝게`, `어둡게` 테마를 선택할 수 있고, `어둡게`에서 앱 영역이 다크 테마로 전환됩니다.
- D단에서 22 km/h로 주행했을 때 Pleos 플랫폼의 `주행 중에는 사용할 수 없습니다.` 차단 화면이 표시됩니다.
- P단 복귀 후 차단 화면이 해제되고 앱 화면이 다시 표시됩니다.

### 주행 제한 결과 해석

PR #11 앱에는 플랫폼의 주행 중 허용 앱으로 처리하기 위한 별도 선언이 없습니다. 이 에뮬레이터에서는 D단 주행 시 앱 내부 제한 UI가 표시되기 전에 Pleos 플랫폼이 앱 전체를 차단했습니다. 따라서 수동 결과는 **플랫폼 수준의 주행 제한 동작 확인**으로 기록합니다.

앱 내부 제한 상태 전환은 아래 연결 UI 테스트의 `actualRestrictionsRemoveAnAlreadyOpenHistoryScreen`와 `missingCarStateNeverShowsSampleDataOrUnlockedControls`로 보완 확인했습니다.

## 2. 연결 UI 테스트

- 실행 대상: Pleos Connect v2.1.0 AVD (`emulator-5554`)
- 결과 시각: `2026-11-10T11:29:23`
- 전체: 6
- 성공: 6
- 실패: 0
- 오류: 0
- 건너뜀: 0
- 종료 코드: 0

`CoachPresentationTest` 5건:

1. `actualRestrictionsRemoveAnAlreadyOpenHistoryScreen`
2. `missingCarStateNeverShowsSampleDataOrUnlockedControls`
3. `previewFocusAndCoachingSessionRespondToInput`
4. `sampleTripOpensDetailsAndReturnsToList`
5. `onlyPhonePreviewOffersAnExitFromSimulatedDrivingRestrictions`

기본 계측 테스트 1건:

1. `ExampleInstrumentedTest.useAppContext`

원본 결과는 `connected-ui-test-results.xml`, 테스트별 원본 로그는 `connected-test-logcat/`에 보관했습니다. 원본 로그에서 `FATAL EXCEPTION`, 앱 프로세스의 치명적 `AndroidRuntime` 오류, ANR 또는 프로세스 종료 패턴은 발견되지 않았습니다.

## 3. 환경 제약 및 미실시 항목

- 이 Pleos Connect v2.1.0 이미지에는 `hmg_car_service`가 등록되어 있지 않습니다. `adb shell cmd hmg_car_service ...`는 `Can't find service: hmg_car_service`를 반환하므로 직접적인 주행 제한 주입은 사용할 수 없습니다.
- 플랫폼 차단이 앱보다 먼저 적용되므로 앱 내부 제한 화면을 수동으로 직접 확인하지 못했습니다. 해당 상태 전이는 연결 UI 테스트로 확인했습니다.
- 실제 차량에서는 검증하지 않았습니다.
- 분할 창, 글자 크기 200%, 영어 로케일, Compose Preview의 모든 상태에 대한 시각 검증은 이번 증빙 범위에서 미실시입니다.
- 일반 Android 기기에서 Mock 주행 상세 화면을 수동으로 확인하는 시각 검증은 미실시입니다. Mock 상세 화면의 열기/복귀 동작은 연결 UI 테스트에 포함되어 있습니다.
- 실제 차량 속도 데이터를 저장해 주행 기록으로 만드는 기능은 PR #11의 구현 범위가 아닙니다. 따라서 Pleos 환경에서 주행 기록이 비어 있는 것은 이번 PR 기준의 예상 동작입니다.
- 40 km/h 주행 과정은 이번 판단에 추가 정보를 주지 않아 증빙 파일에 포함하지 않았습니다.

## 4. 결론

PR #11의 주요 화면 전환, 차량 환경에서의 Mock 데이터 비노출, 테마 전환, 플랫폼 수준의 주행 제한 및 해제, 연결 UI 테스트는 확인되었습니다. 다만 접근성·반응형·다국어 시각 검증과 실제 차량 검증은 별도 후속 확인 항목으로 남습니다.
