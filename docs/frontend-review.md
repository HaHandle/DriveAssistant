# 프론트엔드 검토 안내

## 개발 계획 1단계: UI 전용 데이터 모델

1단계의 대상은 `ui/model/DriverOverview.kt`와 이 문서입니다. 모델은 기존 커밋 `ab09762`에 구현되어 있으며, 아래 표로 네 목표 화면의 표시 항목과 모델 필드를 대응시킵니다. 서버 DTO나 실제 평가 정책을 정의하는 계약이 아닙니다.

| 목표 화면 | 표시할 항목 | 모델 필드 | 데이터가 없을 때 |
| --- | --- | --- | --- |
| 점수 대시보드 | 현재·이전 점수, 변화량, 점수 추이 | `score.current`, `score.previous`, `score.change`, `score.trend` | 미측정은 null. 현재 또는 이전 값이 없으면 변화량도 null |
| 운전 습관 분석 | 정차·신호 시작·유지·소등의 평가와 관찰 근거 | `habits[].kind`, `status`, `observations.met`, `observations.observed` | `UNOBSERVED` 또는 누락된 관찰 근거를 표시하며 0점으로 환산하지 않음 |
| 운전자 숙련도 | 숙련도 미정 상태 또는 화면 구성용 레벨 | `progress`의 `Unassigned` 또는 `Preview.level` | 기본값은 `Unassigned`. 실제 운전자 레벨을 부여하지 않음 |
| 티어 시각화 | 예시 티어 라벨, 예시 경험치와 진행률 | `Preview.tierLabel`, `earnedWithinTier`, `requiredWithinTier`, `fraction` | 실제 기준은 미정. 최고 티어 예시는 다음 목표와 진행률을 null로 구분 |

### 가정과 보존할 의미

- 점수 0~100은 기존 `LiquidScore`의 표시 범위를 따른 UI 가정입니다. 산정식·가중치·서버 응답 형식을 확정하지 않습니다. 측정된 0점은 null과 다릅니다.
- 점수 추이의 `completedAtMillis`는 UI가 받는 밀리초 시각 값입니다. 서버 필드 이름이나 전송 형식을 의미하지 않습니다.
- 관찰 횟수는 `0 <= met <= observed`를 만족합니다. 0회 관찰과 관찰 후 0회 충족은 구분하며, 서로 다른 연수의 횟수를 종합 점수로 합산하지 않습니다.
- 숙련도·티어 명칭, 승급 기준과 경험치 정책은 미정입니다. `Preview`는 배치 검토 전용이며 `RECORDED` 출처와 결합할 수 없습니다.
- 모델에는 Android, Compose, 차량 SDK, 네트워크 또는 직렬화 의존성이 없습니다. 이후 연동 시 별도의 매핑 계층에서 기존 평가 결과를 전달합니다.

### 1단계 완료 기준과 검증

- 네 목표 화면의 표시 항목이 위 모델 필드에 대응하고, 미측정·관찰 부족·티어 미정의 의미가 구분되어야 합니다.
- 표시 범위, 점수 비교 방향, 관찰 횟수, 임시 티어의 출처 제한, 예시 진행률의 경계값을 `DriverOverviewTest`로 검증합니다.
- 검증 명령: `:app:testDebugUnitTest --tests com.example.drivingcoach.ui.model.DriverOverviewTest`. Windows 한글 경로의 실행 옵션은 README를 따릅니다.
- 실행 결과: 모델 단위 테스트 8개 통과, 실패 0개·오류 0개. Gradle 실행이 성공했습니다.
- 이번 검토에서는 이미 존재하는 Mock 공급원·Preview, ViewModel, 화면 연결, 차량 연결을 변경하거나 후속 단계로 진행하지 않습니다.

## 이번 변경

기본 Hello Android 화면을 코칭·주행 기록·설정 화면으로 교체했습니다. 화면 탐색과 선택값은 구성 변경 시 복원되며, 테마·모션·터치 피드백·코칭 모드는 앱을 다시 실행해도 유지됩니다.

| 화면 | 확인할 동작 |
| --- | --- |
| 코칭 | 균형·부드러움·효율 선택, 안내 변경, 코칭 미리보기 시작·종료 |
| 주행 기록 | 예시 주행 3개, 상세 점수와 주행 안내, 목록 복귀 |
| 설정 | 자동·밝게·어둡게, 모션·진동 설정 저장 |
| 주행 제한 | 상세 화면 즉시 제거, 탐색 비활성화, 정적인 핵심 안내 |
| 차량 상태 미확인 | 상세 화면·모션을 제한하고 상태 확인 안내 |
| 차량 데이터 미연결 | 예시 점수를 표시하지 않고 연결 대기 |

일반 Android 기기에서만 예시 데이터와 주행 제한 미리보기를 제공합니다. 미리보기 시작은 실제 센서 수집이나 주행 기록을 시작하지 않습니다. 차량의 주행 데이터와 백엔드 연결은 아직 없습니다.

## 디자인 기준과 구현

공식 문서를 확인한 날짜: 2026-11-10.

- [Pleos Connect UI Design Guide](https://document.pleos.ai/docs/connect/guide/docs-design/Introduction/intro): 사용성·안전성, 일관성, 즉시성, 대비 기준
- [주행 안전을 고려한 설계](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driver-distraction-guideline): 주의 분산과 반복 모션 제한
- [Typography](https://document.pleos.ai/docs/connect/guide/docs-design/foundations/typography): 중립적인 서체와 Asta Sans
- [Basic button](https://document.pleos.ai/docs/connect/guide/docs-design/components/actions/basic-button): 명확한 영역·상태·크기
- [Animation](https://document.pleos.ai/docs/connect/guide/docs-design/foundations/animation): 상태 설명을 위한 움직임
- [앱 구조 및 동작 모델](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/app-structure-screen-model): 멀티 윈도우와 생명주기
- [주행 규제 개발 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide): 플랫폼 UX 제한 신호 반영

본문·보조 글자는 18sp 이상이며 주요 버튼과 선택 영역의 최소 높이는 80dp입니다. 아이콘에는 텍스트 라벨을 함께 제공합니다. 테마별 주요 텍스트 조합의 계산된 대비는 5.7:1 이상입니다. 실제 디스플레이에서의 물리적 글자 크기와 시인성은 팀원 기기 검토가 필요합니다.

배경과 표면은 무채색 중심으로 구성하고, 코칭 정보에 절제된 녹색을 사용했습니다. Asta Sans 원본과 라이선스를 함께 포함했습니다. 자동 테마는 시스템의 주·야간 설정을 따릅니다.

창 너비 840dp 이상·높이 480dp 이상에서는 측면 탐색을 사용합니다. 글자 배율이 1.3을 넘으면 유연하게 높이가 늘어나는 하단 탐색을 사용합니다. 본문은 사용 가능한 너비 940dp 이상에서 두 열로 배치됩니다. 시스템 UI 영역은 safe drawing inset으로 확보하며 시스템 Driving view나 GNB를 재현하지 않습니다.

## 리퀴드 모션

모든 모션은 입력·상태 변경에 따라 한 번 실행됩니다. 반복 타이머·무한 애니메이션·자동 스크롤은 없습니다.

| 요소 | 시간 | 동작 |
| --- | --- | --- |
| 선택 캡슐 | 선행 180ms / 후행 240ms | 이동 방향에 따라 가장자리 속도를 달리해 늘어났다 안착 |
| 코칭 점수 면 | 240ms | 코칭 모드를 바꾸면 곡면만 변화하며 점수는 유지 |
| 버튼 | 120ms / 160ms | 누름에 따라 작은 압축과 모서리 변화 |
| 화면·안내 | 진입 180ms / 퇴장 90~100ms | 짧은 페이드로 문맥 전환 |

사용자 모션 설정을 끄거나 앱이 백그라운드로 이동하면 모션을 멈춥니다. Compose 애니메이션은 시스템 애니메이션 배율도 따릅니다. 주행 제한 또는 차량 상태 미확인 시에는 애니메이션 중간값 대신 목표값을 바로 렌더링합니다.

## 차량 연동 범위

AAOS에서만 optional `android.car` 라이브러리를 사용합니다. `CarUxRestrictionsManager`의 현재 값을 읽고 리스너로 변경을 받습니다. DO 필요 신호 또는 하나 이상의 활성 제한이 있으면 상세 기능 전체를 보수적으로 제한합니다. Car service 단절·조회 실패·백그라운드 전환 시에는 미확인 상태로 돌아갑니다.

휴대전화 미리보기 설정은 실제 차량 상태를 덮어쓰지 않습니다. 주행 제한이 걸리면 열려 있던 상세 화면을 즉시 제거하고 코칭 첫 화면으로 초기화합니다.

이 커밋은 distraction-optimized 메타데이터를 선언하지 않습니다. 실제 주행 중 앱 실행 허용 여부는 플랫폼·지역·앱 분류 정책에 따릅니다. 플랫폼 심사나 실차 검증 완료를 의미하지 않습니다.

## 검증 결과와 이어서 확인할 항목

- 디버그 APK 빌드 통과
- JVM 단위 테스트 6개 통과: 기존 1개, 표시 정책 검증 5개
- Android UI 테스트 5개 추가 및 테스트 APK 컴파일 통과
- Android Lint 통과. 기본 색 리소스 미사용, 라벨 중복, 의존성 새 버전 안내 경고는 남아 있습니다.
- 에뮬레이터가 부팅을 완료하지 못해 기기 UI 테스트·실제 렌더링·차량 서비스 연결은 실행 확인하지 못했습니다. 요청에 따라 부팅 문제 조사는 중단했습니다.

### 팀원 확인 순서

1. 일반 Android 기기에서 실행해 미리보기 및 예시 데이터 표시를 확인합니다.
2. 코칭 모드를 양방향으로 바꾸며 캡슐 이동과 안내 변경을 확인합니다.
3. 코칭 미리보기를 시작하고 종료합니다. 실제 주행 기록이 생성되지 않는지 확인합니다.
4. 주행 기록을 열어 상세 화면과 뒤로 가기를 확인합니다.
5. 테마와 모션·진동 설정을 바꾸고 앱을 재실행해 저장 여부를 확인합니다.
6. 설정의 주행 제한 미리보기에서 상세 화면과 탐색이 제한되는지, 종료 후 코칭으로 돌아오는지 확인합니다.
7. 세로·가로·분할 창과 글자 배율 200%에서 스크롤 및 주요 조작 도달 가능성을 확인합니다.
8. AAOS/Pleos 환경에서 실제 UX 제한·Car service 단절·포그라운드 복귀를 검증합니다. 예시 데이터가 노출되지 않아야 합니다.

기기가 연결된 환경에서 UI 테스트를 실행할 수 있습니다.

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

Windows 한글 경로 환경에는 README의 실행 옵션을 함께 적용합니다.

## 주요 파일

- `ui/DrivingCoachApp.kt`: 앱 상태·설정 저장·테마·탐색·Preview
- `ui/CoachScreens.kt`: 코칭·기록·설정·제한 화면
- `ui/CoachComponents.kt`: 선택 캡슐·점수 곡면·버튼·아이콘
- `ui/theme/`: Asta Sans·색상·글자 위계
- `vehicle/VehicleUxState.kt`: 데이터 노출·상세 화면·모션 표시 정책
- `vehicle/AutomotiveUxConnection.kt`: AAOS UX 제한 연동
