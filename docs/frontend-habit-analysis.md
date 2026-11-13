# 운전 습관 목록과 상세 화면

후속 프론트엔드 계획의 목록 연결·상세 화면·탐색 단계입니다. 선행 PR #16의 표시 모델을 실제 코칭 화면에 연결합니다. 새 탭이나 라이브러리 없이 기존 테마, CoachPanel, StatusPill, LiquidAction, PresentationPolicy를 재사용합니다.

## 사용자 흐름과 데이터

일반 기기의 예시 코칭 화면에서 정차 제어·신호 미리 켜기·유지·소등 항목을 누르면 해당 습관의 상세 화면을 엽니다. 평가와 관찰 근거를 별도 카드로 표시하며 넓은 창은 두 열, 좁은 창과 큰 글자는 한 열을 사용합니다. 상단의 목록 복귀 버튼이나 시스템 뒤로 가기로 돌아옵니다.

Repository → ViewModel → StateFlow → Compose 경계를 유지합니다. 화면은 기존 Ready 상태의 habits만 읽으며 Mock fixture, 저장소·서버 인터페이스, 원본 평가 모델을 바꾸지 않습니다. 차량의 실제 공급원은 여전히 연결 대기이며 자동차 환경에서는 주입된 Mock으로도 이 상세 화면에 들어갈 수 없습니다.

목록과 상세는 같은 `HabitAnalysisPresentation`을 사용합니다. [표시 규칙](frontend-habit-presentation.md)의 근거 미제공, 관찰 0회, 관찰 후 충족 0회, 충족 관찰 있음과 별도의 평가 보류를 유지합니다. 0/0을 횟수나 비율로 표시하지 않으며 횟수를 점수·백분율·티어로 환산하지 않습니다. 근거 없는 MET을 표시 보류하는 규칙도 선행 단계의 임시 표시 정책 그대로입니다.

## 상태 복원과 제한

- 선택은 목록 위치가 아닌 HabitKind.name 문자열만 rememberSaveable로 저장합니다. 원본 평가 객체나 목록은 저장하지 않습니다.
- 선택한 키를 현재 목록에 다시 대입합니다. 재정렬 후에도 같은 항목이며, 같은 종류의 관찰 횟수가 갱신되면 현재 값을 표시합니다.
- 삭제된 종류, 알 수 없는 저장 키, 중복된 종류처럼 대상을 하나로 정할 수 없는 경우는 상세를 표시하지 않습니다.
- 홈 스크롤은 상세보다 상위에서 유지해 목록 복귀 시 위치를 보존합니다. 기존 점수 기간 선택과 설정 저장 키는 유지합니다.
- Ready 데이터가 있는 상태의 저장 상태 복원에서는 선택한 상세를 복원합니다. 로딩·빈 기록·오류에 진입하거나 항목이 사라지면 선택을 해제하며 데이터 복구만으로 다시 열지 않습니다.
- 탭을 바꾸면 선택을 해제합니다. 차량 제한·상태 미확인에서는 기존 제한 화면을 즉시 표시합니다.
- 상세 표시 자격과 현재 항목을 전환 애니메이션 바깥에서 검사합니다. 제한·데이터 변경 시 퇴장 애니메이션이 이전 상세 내용을 남기지 않도록 전환 자체를 제거합니다.

## 디자인·모션·접근성

기존 주·야간 색상과 Asta Sans 글자 체계를 사용합니다. 습관 행과 복귀 버튼은 최소 80dp이며 중요한 문구는 큰 글자에서 잘리지 않도록 줄바꿈과 세로 스크롤을 허용합니다. 제목·구획에는 heading, 습관 행에는 버튼 역할과 상세 진입 동작 라벨을 제공합니다. 평가는 색상과 함께 텍스트로 표시하고 장식 아이콘은 별도로 읽지 않습니다.

일반적인 상세 진입·복귀는 기존 앱과 같은 180ms 진입·100ms 퇴장 페이드입니다. 복귀 버튼은 기존 LiquidAction의 120ms 눌림·160ms 곡면 피드백을 사용합니다. 데이터 변경으로 자동 전환·반복 모션을 만들지 않습니다. 모션 끄기·백그라운드·차량 제한에서는 기존 정책에 따라 목표 상태를 바로 표시합니다.

참고: [Pleos UI 디자인 원칙](https://document.pleos.ai/docs/connect/guide/docs-design/Introduction/intro), [Animation](https://document.pleos.ai/docs/connect/guide/docs-design/foundations/animation), [주행 안전 설계](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driver-distraction-guideline), [Compose 상태 저장](https://developer.android.com/develop/ui/compose/state-saving). 모션 시간은 프로젝트의 기존 구현을 재사용한 값이며 공식 인증을 의미하지 않습니다.

## 변경 파일

| 영역 | 파일 |
| --- | --- |
| 공통 표시·상세 | ui/HabitAnalysisContent.kt, ui/HabitAnalysisScreen.kt |
| 목록·탐색 연결 | ui/CoachScreens.kt, ui/DrivingCoachApp.kt |
| 현재 데이터 선택 | ui/model/HabitDetailSelection.kt |
| 문구·Preview | 한글·영문 strings.xml, ui/preview/HabitAnalysisPreview.kt |
| 테스트 | JVM HabitDetailSelectionTest.kt, 기기 HabitAnalysisScreenTest.kt |
| 인계 | README.md, frontend-habit-presentation.md, 이 문서 |

## Preview와 검증

Android Studio에서 `ui/preview/HabitAnalysisPreview.kt`의 Habit analysis 그룹을 엽니다. 정상 네 항목, 근거 미제공, 관찰 0회, 관찰 후 충족 0회, 횟수가 있으나 미평가, 성공 평가와 충족 0회의 불일치를 포함한 9개 사례입니다. 주간·야간·320dp/글자 200%·넓은 영문 화면을 제공합니다. 기존 App states Preview에서도 새 목록을 확인할 수 있습니다.

완료 기준:

- 네 습관이 동일한 규칙으로 목록과 상세에 표시되고 항목 진입·복귀·상태 복원이 연결됨.
- 데이터 갱신·삭제·비 Ready 상태에서 오래된 평가를 유지하지 않고 차량의 Mock 차단을 보존함.
- JVM의 선택 키·최신 값·삭제·중복 경계 검증과 기존 회귀 테스트가 통과함.
- 디버그 앱·Preview 컴파일, Lint, UI 테스트 APK 컴파일이 오류 없이 완료됨.
- 실제 렌더링·기기 테스트 실행 여부를 컴파일 결과와 구분해 기록함.

최종 코드에서 `:app:assembleDebug`, `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebugAndroidTest`가 모두 통과했습니다. JVM은 기존 68개와 신규 선택 검증 5개를 합쳐 73개이며 실패·오류·건너뜀은 0개입니다. Lint 오류는 0개이며 기존 경고 19개가 남아 있습니다. 신규 습관 화면·선택·테스트 파일에는 Lint 경고가 없습니다.

Compose UI 회귀 테스트는 13개를 추가했습니다. 항목별 진입·복귀, 스크롤·기간 유지, 시스템 뒤로 가기, 선택 복원, 현재 횟수 갱신, 비 Ready 상태·삭제·차량 제한·탭 이동 시 상세 제거, 누락·0회·미충족·평가 보류, 퇴장 페이드 중 백그라운드 전환을 다룹니다. UI 테스트 APK는 컴파일만 확인했으며 기기에서 실행하지 않았습니다.

초기 #17 검증에서는 Preview 정의와 컴파일만 확인했습니다. 후속 #18에서 Ubuntu CI로 실제 렌더링을 수행한 결과와 대표 이미지 검토는 [2026-11-12 증빙](preview-evidence/2026-11-12/README.md)에 기록했습니다. TalkBack, 물리적 글자 크기·터치 영역, 실차 제한 전환은 추가 검토 대상입니다. 에뮬레이터 부팅 문제는 조사하지 않았습니다.

## 2026-11-13 선행 동기화와 검토

앱 코드 검증 대상은 `57d0e94968059012ec2c2cca789516c583fc0889`입니다. 병합된 main `9dc07b2`를 merge commit으로 반영해 #15의 기간 선택 전용 모션·모션 비허용 시 전환 제거, #16의 표시 모델·검증 개수 정정, #19의 PR #12 증빙을 보존했습니다. PR #17의 base는 main이며 고유 변경은 기존 13개 파일입니다.

해당 커밋을 별도 작업 디렉터리에서 빌드했습니다. `assembleDebug`, `testDebugUnitTest`, `lintDebug`, `assembleDebugAndroidTest`가 모두 성공했고 83개 Gradle 작업을 실제 실행했습니다. JVM 73개는 실패·오류·건너뜀 0개입니다. Lint 오류 0개·기존 경고 19개이며 UI 테스트 APK는 컴파일만 확인했습니다.

현재 기기 조회에서 연결된 ADB 기기가 없었습니다. 에뮬레이터를 새로 부팅하거나 부팅 문제를 조사하지 않았고, `HabitAnalysisScreenTest` 13개와 TalkBack을 실행 통과로 기록하지 않습니다. 선택 키 재조회·삭제/중복 거부·차량 Mock 차단·진행 중 상세 페이드 제거는 코드와 기존 JVM 경계 테스트를 대조했으며, 실제 탐색·상태 복원·스크롤·모션 중단의 실행 확인은 남아 있습니다.

실행 가능한 팀원 환경에서는 이 PR의 최신 앱 코드로 다음 테스트를 확인합니다. Windows 경로 옵션과 JDK/SDK 설정은 README의 빌드 안내를 따릅니다.

```text
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.drivingcoach.HabitAnalysisScreenTest,com.example.drivingcoach.ScoreTrendPanelTest
```

습관 화면 13개와 점수 추이 회귀 6개의 결과를 구분하고 대상 커밋·기기/OS·실행 명령·JUnit 결과를 남깁니다. 실패가 있으면 원인과 재현 조건을 먼저 기록합니다. 오래된 #12 차량 캡처와 #18의 기존 정적 이미지가 이번 동적 테스트를 대신하지는 않습니다. 최신 소스의 정적 이미지 재생성은 #18의 선행 동기화 후 확인합니다.

## 팀원 리뷰 요청

- 목록과 상세에서 누락·관찰 0회·관찰 후 미충족·평가 보류 문구가 명확히 구분되는지 확인해 주세요.
- 큰 글자·좁은 창·영문에서 제목, 평가, 횟수와 복귀 버튼을 읽고 스크롤할 수 있는지 확인해 주세요.
- 목록 복귀 후 위치와 점수 기간이 유지되고 시스템 뒤로 가기가 같은 동작인지 확인해 주세요.
- 모션 끄기·백그라운드·데이터 변경·차량 제한 시 이전 상세가 남지 않는지 확인해 주세요.
- TalkBack 읽기 순서와 행의 상세 진입 동작 안내를 대상 환경에서 확인해 주세요.
