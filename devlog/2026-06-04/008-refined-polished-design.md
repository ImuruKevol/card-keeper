# 008. 인증 화면과 관리 화면의 절제된 고급 디자인 개선

## 사용자 원 요청

```text
아직 너무 안 예뻐. 세련되면서 디자인을 최대한 예쁘고 깔끔하면서 너무 과하지는 않게 개선해줘.

리뷰 ID: hfcdnrphuhohqdptaeemafirwucolqjj
제목: 디자인 수정
요청 링크: https://bus.sub.nanoha.kr/
```

## 변경 요약

- 로그인/가입 화면을 전체 화면 반반 분할에서 중앙 인증 시트 형태로 재구성했다.
- 브랜드 패널의 명함 비주얼, 그라데이션, 여백, 모바일 배치를 더 절제된 형태로 조정했다.
- 공통 상단 앱 셸의 높이, 로고 크기, 내비게이션, 사용자 칩, 버튼 반경과 그림자를 더 가볍게 정리했다.
- 명함 관리와 사용자 관리 화면의 주요 패널/테이블/모달/모바일 카드 반경과 그림자를 낮춰 과한 카드 느낌을 줄였다.

## 변경 파일

- `src/app/page.access/view.scss`
- `src/app/layout.sidebar/view.scss`
- `src/app/page.cards/view.scss`
- `src/app/page.users/view.scss`
- `devlog.md`
- `devlog/2026-06-04/008-refined-polished-design.md`

## 검증 결과

- `wiz_project_build(clean=false, projectName="main")`: 성공
- 개발 쿠키 `season-wiz-project=main`, `season-wiz-devmode=true` 포함 로컬 브라우저 검증:
  - `http://127.0.0.1:3000/access` 데스크톱 1440x900 스크린샷 확인
  - `http://127.0.0.1:3000/access` 모바일 393x852 스크린샷 확인
- `PLAYWRIGHT_BASE_URL=http://127.0.0.1:3000 npx playwright test`: 데스크톱 Chromium 3개 + 모바일 Chromium 3개, 총 6개 smoke test 통과

## 남은 리스크

- 이번 요청에는 로그인 정보가 없어 인증 후 `/cards`, `/users` 실제 데이터 화면은 브라우저로 직접 로그인 검증하지 못했다.
- 현재 작업트리에는 이전 작업으로 보이는 미커밋 변경이 다수 있어, 이번 디자인 범위 외 파일은 정리하지 않았다.
