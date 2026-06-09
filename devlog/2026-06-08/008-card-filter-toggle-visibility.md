# 명함 목록 필터 토글 선택 상태 시인성 개선

- **ID**: 008
- **날짜**: 2026-06-08
- **리뷰 ID**: kivsqhpmuzfldxjyjomqgzrxcrwfklmr
- **유형**: UX 개선

## 작업 요약

명함 목록 필터 바의 선택된 토글 상태가 더 명확히 보이도록 활성 버튼을 기본 강조색 배경과 흰색 텍스트로 변경했다.
정렬 기준 `최신순/이름순/회사순`도 select 대신 검색 범위/정렬 방향과 같은 segmented toggle 버튼으로 교체했다.

## 원문 요청사항

```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

- 토글 버튼들의 UI가 현재 선택된 값이 뭔지 알아보기 힘들게 디자인이 되어있어. 알아보기 쉽게 수정해줘.
- 최신/이름/회사순 select도 토글 버튼 식으로 스타일을 수정해줘.

## 리뷰 요약

- 리뷰 ID: kivsqhpmuzfldxjyjomqgzrxcrwfklmr
- 제목: UI 추가 개선
- 요청 링크: https://bus.sub.nanoha.kr/
- Codex 요청자: 권태욱
- 프로젝트 루트: /opt/app
```

## 변경 파일 목록

- `src/app/page.cards/view.ts`
  - 정렬 기준 토글 클릭용 `setSort` 헬퍼 추가
- `src/app/page.cards/view.pug`
  - 정렬 기준 select를 `최신순/이름순/회사순` segmented toggle로 교체
- `src/app/page.cards/view.scss`
  - segmented toggle 활성 상태 대비 강화
  - 정렬 기준 토글 너비와 모바일 반응형 기준 추가
  - 사용하지 않는 `filter-select` 스타일 제거
- `devlog.md`
- `devlog/2026-06-08/008-card-filter-toggle-visibility.md`

## 검증 결과

- `wiz_project_build(clean=false, projectName="main")`: 성공
- `npm run test:e2e -- --project=chromium --project=mobile-chromium`: 성공, 개발 쿠키 `season-wiz-project=main`, `season-wiz-devmode=true` 적용된 smoke test 6개 통과

## 남은 리스크

- 실제 로그인 계정 비밀번호가 없어 `/cards` 로그인 후 필터 바의 활성 상태를 브라우저에서 직접 조작 검증하지 못했다.
