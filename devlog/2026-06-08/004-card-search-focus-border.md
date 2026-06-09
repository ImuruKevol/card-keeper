# 명함 목록 검색 입력 focus 강조 테두리 제거

- **ID**: 004
- **날짜**: 2026-06-08
- **리뷰 ID**: hfaceqgtvkzkyxfxbvbjwyxdfmrbxcuq
- **유형**: 버그 수정

## 작업 요약

명함 목록 검색 input이 focus 되었을 때 파란색처럼 보이는 강조 border와 focus ring이 생기지 않도록 수정했다.
`.search-field:focus-within` 상태에서도 기본 border 색상을 유지하고 `box-shadow`를 제거했다.

## 원문 요청사항

```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

input에 focus 시 파란색 border가 생기는데 없애줘

## 리뷰 요약

- 리뷰 ID: hfaceqgtvkzkyxfxbvbjwyxdfmrbxcuq
- 제목: 검색 input 버그
- 요청 링크: https://bus.sub.nanoha.kr/access
- Codex 요청자: 권태욱
- 프로젝트 루트: /opt/app
```

## 변경 파일 목록

- `src/app/page.cards/view.scss`
  - `.search-field:focus-within`의 강조 border 색상 제거
  - focus ring 역할의 `box-shadow` 제거
- `devlog.md`
- `devlog/2026-06-08/004-card-search-focus-border.md`

## 검증 결과

- `wiz_project_build(clean=false, projectName="main")`: 성공
- `curl -b "season-wiz-project=main; season-wiz-devmode=true" http://127.0.0.1:3000/cards`: 200 OK
- 빌드 산출물 `project/main/build/dist/build/main.js`에서 `page.cards` 스타일의 `.search-field:focus-within`이 `border-color: #ccd8d3`, `box-shadow: none`으로 반영된 것 확인

## 남은 리스크

- 현재 컨테이너에 Playwright/Chromium/Chrome이 없어 실제 브라우저 focus 상태 스크린샷 검증은 수행하지 못했다.
