# DriveAssistant

PLEOS 차량 내 디스플레이에서 초보 운전자를 단계별로 코칭하는 운전 연수 애플리케이션입니다.

## Training curriculum

0. 주행 준비
1. 출발 및 정차
2. 직선 도로 주행
3. 곡선 도로 주행
4. 차선 변경
5. 교차로 주행
6. 주차

## Source layout

- `domain`: 공통 모델과 단계 평가 계약
- `feature`: 준비 과정과 6개 연수 단계별 규칙
- `application`: 연수 세션 및 코칭 메시지 조정
- `port`: 차량 데이터, 피드백, 기록 저장 경계
- `infrastructure`: PLEOS SDK와 저장소 구현
- `presentation`: 대시보드, 주행 중 코치, 주행 후 복기 화면

자세한 설계 원칙은 [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)를 참고하세요.
