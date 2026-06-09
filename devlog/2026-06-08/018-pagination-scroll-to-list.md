# 페이지 이동 후 목록 상단 스크롤 적용

- 작업 ID: 018
- 날짜: 2026-06-08
- 리뷰 ID: kivsqhpmuzfldxjyjomqgzrxcrwfklmr
- 분류: UX

## 원 요청

페이지 번호를 클릭해서 페이지 이동 시 pc/모바일 모두 표 첫 번째 사람의 정보가 보이도록 스크롤을 이동해줘.

## 변경 요약

- 명함 목록 바로 위에 스크롤 기준 앵커를 추가했다.
- 페이지네이션 이동 후 목록 데이터 렌더링이 끝나면 해당 앵커로 스크롤되도록 처리했다.
- 스크롤 기준점에 마진을 부여해 PC/모바일 모두 첫 항목 영역이 화면에 보이도록 조정했다.

## 변경 파일

- `src/app/page.cards/view.pug`
- `src/app/page.cards/view.ts`
- `src/app/page.cards/view.scss`
- `devlog.md`
- `devlog/2026-06-08/018-pagination-scroll-to-list.md`

## 검증

- `wiz_project_build(projectName="main", clean=false)` 통과
- `npm run test:e2e -- --project=chromium --project=mobile-chromium` 통과, 6 passed

## 남은 리스크

- 인증된 실제 다량 명함 데이터에서 페이지 번호 클릭 후 스크롤 위치를 자동 테스트로 직접 시각 검증하지 못했다.
