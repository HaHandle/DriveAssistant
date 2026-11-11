# PR #11 Pleos 검증 증빙 (`d489f80`)

이 디렉터리는 PR #11의 소스 커밋 `d489f809f1928edf6aa7c1c5cbe96e930f180284`를 Pleos Connect 에뮬레이터에서 검증한 결과입니다. 다른 커밋의 결과와 섞지 않습니다.

## 검증 대상

- PR: `#11`
- 원격 브랜치: `feat/overview-mock-foundation`
- 로컬 검토 브랜치: `review/pr11-frontend`
- 커밋: `d489f809f1928edf6aa7c1c5cbe96e930f180284`
- 패키지: `com.example.drivingcoach`
- 에뮬레이터: Pleos Connect v2.1.0 (`emulator-5554`)
- Android: 14 / API 34
- 실행 일자: 2026-11-10

## 파일 목록

| 파일 | 확인 내용 |
| --- | --- |
| `01-coach-balance.png` | 코칭 화면과 `균형` 선택 상태 |
| `02-coach-smooth.png` | `부드러움` 선택에 따른 문구 전환 |
| `03-coach-efficiency.png` | `효율` 선택에 따른 문구 전환 |
| `04-history-empty.png` | 차량 환경에서 샘플 주행을 노출하지 않는 빈 주행 기록 |
| `05-settings-automatic-light.png` | 시스템이 밝은 상태일 때 `자동` 테마 |
| `06-settings-light.png` | `밝게` 테마 선택 상태 |
| `07-settings-dark.png` | `어둡게` 테마 선택 상태 |
| `08-before-restriction.png` | 주행 제한 전의 앱 표시 상태 |
| `09-platform-blocked-drive-d.png` | D단·22 km/h에서 Pleos 플랫폼이 앱 사용을 차단한 상태 |
| `10-restriction-released.png` | P단 복귀 후 앱이 다시 표시된 상태 |
| `connected-ui-test-results.xml` | 연결 UI 테스트 6건의 원본 JUnit 결과 |
| `connected-ui-test-exit-code.txt` | Gradle 연결 테스트 종료 코드 `0` |
| `connected-test-logcat/` | 연결 UI 테스트별 원본 Logcat 6개 |
| `validation.md` | 확인 결과, 해석 및 미실시 범위 |

## 핵심 결과

- 연결 UI 테스트: `6/6` 성공
- 실패/오류/건너뜀: `0/0/0`
- 수동 화면 확인: 코칭 선택 3종, 빈 주행 기록, 테마 3종, 주행 제한 진입 및 해제
- 주행 제한: 앱 내부 화면보다 먼저 Pleos 플랫폼의 차단 화면이 표시됨

세부 판단과 검증 한계는 [`validation.md`](validation.md)를 확인합니다.
