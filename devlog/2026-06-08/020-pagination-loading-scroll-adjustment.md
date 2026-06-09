# 페이지 이동 로딩 표시 및 즉시 스크롤 보정

- 작업 ID: 020
- 날짜: 2026-06-08
- 리뷰 ID: kivsqhpmuzfldxjyjomqgzrxcrwfklmr
- 분류: UX

## 원 요청

- 모바일에서는 스크롤 위치가 조금 덜올라가는 버그가 있어.
- 스크롤이 올라갈 때 지연 시간이 과도하게 길어.
- 페이지 이동 시 기존 목록은 비우고 로딩 중 표시가 뜨도록 해줘.

## 변경 요약

- 페이지네이션 이동 시 기존 명함 목록을 즉시 비우고 로딩 상태를 먼저 렌더링하도록 변경했다.
- 로딩 상태 렌더 직후 목록 상단으로 스크롤되도록 처리해 데이터 응답 대기 시간만큼 스크롤이 늦어지지 않도록 했다.
- 스크롤 동작을 smooth에서 즉시 이동으로 변경했다.
- 스크롤 기준점의 상단 여백을 제거해 모바일에서 덜 올라가는 현상을 줄였다.

## 변경 파일

- `src/app/page.cards/view.ts`
- `src/app/page.cards/view.scss`
- `devlog.md`
- `devlog/2026-06-08/020-pagination-loading-scroll-adjustment.md`

## 검증

- `wiz_project_build(projectName="main", clean=false)` 통과
- `npm run test:e2e -- --project=chromium --project=mobile-chromium` 통과, 6 passed

## 남은 리스크

- 인증된 실제 다량 명함 데이터에서 페이지 이동 중 로딩 표시와 모바일 스크롤 위치를 자동 테스트로 직접 시각 검증하지 못했다.
