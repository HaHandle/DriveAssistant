# Pleos Connect UI 적용 현황

기준: 2026-11-05 공식 웹 디자인 가이드. 이 문서는 적용 근거이며 공식 검수 통과 증명서가 아니다.

| 항목 | 구현 | 확인 범위 |
|---|---|---|
| 글꼴 | 공식 가이드가 연결한 Asta Sans, Regular 400 / Bold 700 정적 인스턴스 | Google Fonts 원본 및 OFL 동봉 |
| 글자 위계 | Title 40/52, 36/44, 32/40; Body 30/38, 28/36, 26/34; Label 24/28, 20/24 | 가이드 표의 크기/행간을 sp로 매핑. 실제 차량 DPI/글자 물리 높이는 미확인 |
| 주야간 | 시스템 night mode에 따라 고정 토큰 전환, dynamic color 제거 | 차량의 주야간 신호와 Android configuration 연동은 미확인 |
| 색상 | Basic00/50/100/500/600/900 공식 hex 사용 | 주요 글자 및 버튼 대비 계산. 미사용 Material 역할은 기본값 유지 |
| 창 크기 | 앱 가용 창 840dp 이상 2열, 이하는 세로; 스크롤과 시스템 inset 유지 | 840dp 분기는 앱 설계 선택, 공식 고정 규격 아님 |
| 시스템 영역 | 시스템 status/GNB를 모사하거나 덮지 않음 | 실제 S/W/WF 차량별 창 및 inset 검토 필요 |
| 조작 | 기본 버튼 80dp 이상, 보조 버튼 64dp 이상, 단일 주요 동작 | 높이/모서리는 앱 설계 선택; 공식 Figma 치수와 1:1 일치 판정 전 |
| 버튼 상태 | 누름/비활성 시 색상 분리, 데이터 재생 중 비활성+문자 안내 | 시간 측정 및 장치 터치 검수는 미실시 |
| 주행 제한 | 기존 CarUxRestrictions 기반 제한 유지, 제한/상태 미확인 시 자세한 정보 숨김 | 이번 변경에서 차량 로직 수정 및 기능 검증 안 함 |
| 반응형 Preview | 840x480, 600x960, 1280x720, 야간, fontScale 1.3 | Preview 정의만 추가. 렌더링/스크린샷 검수 미실시 |

## 주요 대비 비율

WCAG 상대 휘도 공식으로 sRGB hex를 계산했다. 런타임 화면 측정 결과는 아니다.
주간 본문 #131417 / #F7F8FA, 보조 글자 #44464E / #F7F8FA,
기본 버튼 #FFFFFF / #44464E 및 야간 역상 버튼과 보조 글자를 확인한다.
정확한 계산 수치는 PR 설명에 기록한다. 이번 UI에서 사용한 활성 글자 조합은 4.5:1 이상이다.

## 남은 디자인 검수

- 실제 Pleos 앱 창에서 핵심 글자 높이 5mm 이상, 보조 글자 3mm 이상 확인.
- 창 리사이즈/분할 화면/접근성 글자 확대 시 넘침, 문단 3줄 권장 기준 확인.
  긴 결과 피드백은 문장과 최대 감속 정보를 별도 문단으로 표시하고 중요 정보를 말줄임하지 않는다.
- 공식 Figma 컴포넌트의 치수, 상태, 간격과 화면별 대조.
- 실제 화면 밝기/주야간 조건, 터치 피드백, 0.25초 응답 기준 확인.
- 한국어 외 언어 지원, 앱 아이콘, 등록/검수 정책 별도 점검.

사용자의 기능 검증 중단 지시를 유지한다. 이번 작업에서 빌드, 단위/기기 테스트,
에뮬레이터 및 CI를 실행하지 않는다. 디자인 코드 변경은 main이 아닌 별도 PR에 보관한다.

## 출처

- https://document.pleos.ai/docs/connect/guide/docs-design/Introduction/intro
- https://document.pleos.ai/docs/connect/guide/docs-design/getting-started/layout
- https://document.pleos.ai/docs/connect/guide/docs-design/getting-started/window
- https://document.pleos.ai/docs/connect/guide/docs-design/foundations/typography
- https://document.pleos.ai/docs/connect/guide/docs-design/foundations/colors
- https://document.pleos.ai/docs/connect/guide/docs-design/components/actions/basic-button
- Font: https://github.com/google/fonts/tree/main/ofl/astasans
