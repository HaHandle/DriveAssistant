# 속도 기반 정차 연수 통합 검증

대상: 현재 `main`의 Compose 코칭 화면 위에 PR #4와 #8의 순수 감속·반복 정차 분석을 연결한 통합 브랜치.

## 구현 범위

- `analysis/DecelerationSession.kt`와 `StopControlTracker.kt`가 차량 타임스탬프와 m/s 속도를 입력받아 강한 감속 후보, 최대 감속, 속도대별 정차를 계산한다.
- 일반 Android에서는 코칭 홈의 **정차 제어 연수**에서 부드러운 감속, 급한 감속, 반복 정차 데모를 실행한다. 데모 점수는 실제 평가 기준이 아니다.
- Automotive에서는 조작 허용 상태에서 속도 권한을 요청하고 `PERF_VEHICLE_SPEED`를 구독한다. 기존 UX 제한 상태가 허용되고 최근 속도 샘플이 있을 때 연수를 시작한다. 종료와 상세 결과는 허용 상태에서만 제공한다. 기어 P 확인은 아직 연동하지 않았다.
- 기존 대시보드 점수와 주행 기록에 감속 결과를 합산하지 않는다. 세션 기록의 영속 저장은 후속 통합 범위다.

## 자동 검증

- `:app:testDebugUnitTest`: #8에서 옮긴 분석 경계 테스트 14개를 포함해 성공.
- `:app:assembleDebug`: 성공.
- `:app:assembleDebugAndroidTest`: 성공. 새 `SpeedTrainingScreenTest`의 기기 실행은 별도 확인 필요.

## 화면·차량 증빙 요청

통합 커밋의 APK를 설치한 뒤 같은 커밋 SHA를 기록한다. 캡처 파일명 예시:

1. 일반 Android: `01-practice-entry.png` — 코칭 홈의 정차 제어 연수 진입 버튼.
2. 일반 Android: `02-repeated-stop-result.png` — 반복 정차 데모의 후보 1회, 30–60 km/h 정차 3회와 후보 미감지 2/3회.
3. Pleos: `03-speed-permission-and-sample.png` — P 상태에서 속도 권한 허용 후 최근 속도 표시.
4. Pleos: `04-vehicle-result.png` — 실제 속도 이벤트를 수집한 뒤 P 상태에서 종료한 결과. 신호의 단위와 원본 이벤트 시각은 Logcat과 함께 보존.
5. Pleos: `05-driving-restriction.png` — 수집 화면에서 주행 제한 전환 시 상세 정보가 숨겨진 모습과 P 복귀 후 결과 확인.

실제 차량과 Pleos 전용 속도 콜백 주기, 1.2초 정차 추적 간격, 2초 수신 중단 기준은 아직 검증되지 않았다. 데모 화면만으로 차량 연결을 검증했다고 표시하지 않는다.
