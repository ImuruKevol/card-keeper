# 모바일 페이지네이션 5페이지 단위 표시 적용

- 작업 ID: 016
- 날짜: 2026-06-08
- 리뷰 ID: kivsqhpmuzfldxjyjomqgzrxcrwfklmr
- 분류: UX

## 원 요청

페이지네이션 부분이 모바일에서 디자인이 깨지고 있어. 모바일에서는 페이지가 10페이지 단위 말고 5페이지 단위로 해줘.

## 변경 요약

- 모바일 폭에서는 페이지 번호 블록을 5개 단위로 계산하도록 변경했다.
- 데스크톱에서는 기존처럼 10개 단위 페이지 번호를 유지했다.
- 모바일 페이지네이션 버튼 크기, 간격, 정렬을 줄여 좁은 화면에서 깨짐을 줄였다.

## 변경 파일

- `src/app/page.cards/view.ts`
- `src/app/page.cards/view.scss`
- `devlog.md`
- `devlog/2026-06-08/016-mobile-pagination-block.md`

## 검증

- `wiz_project_build(projectName="main", clean=false)` 통과
- `npm run test:e2e -- --project=chromium --project=mobile-chromium` 통과, 6 passed

## 남은 리스크

- 인증된 실제 다량 명함 데이터로 모바일 페이지네이션이 표시되는 화면은 자동 테스트에서 직접 시각 검증하지 못했다.
