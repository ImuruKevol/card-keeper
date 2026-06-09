# 검색 input focus-visible 기본 테두리 보강 제거

- **ID**: 006
- **날짜**: 2026-06-08
- **리뷰 ID**: hfaceqgtvkzkyxfxbvbjwyxdfmrbxcuq
- **유형**: 버그 수정

## 작업 요약

검색 input focus 시 파란색 테두리가 아직 남는 문제를 후속 보강했다.
명함 목록과 사용자 관리의 `search-field` 패턴 모두에서 `focus-within` 강조를 제거하고, input 자체의 `focus`/`focus-visible` outline, border, box-shadow를 명시적으로 제거했다.

## 원문 요청사항

```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

아직도 focus 시 파란색 그 테두리가 남아있어

## 리뷰 요약

- 리뷰 ID: hfaceqgtvkzkyxfxbvbjwyxdfmrbxcuq
- 제목: 검색 input 버그
- 요청 링크: https://bus.sub.nanoha.kr/access
- Codex 요청자: 권태욱
- 프로젝트 루트: /opt/app
```

## 변경 파일 목록

- `src/app/page.cards/view.scss`
  - 검색 input `focus`/`focus-visible` outline, border, box-shadow 제거 보강
  - `search-field:focus-within`에 outline 제거 추가
- `src/app/page.users/view.scss`
  - 사용자 관리 검색 input의 기존 focus 강조 border/ring 제거
  - 검색 input `focus`/`focus-visible` outline, border, box-shadow 제거 보강
- `devlog.md`
- `devlog/2026-06-08/006-search-input-focus-visible-reset.md`

## 검증 결과

- `wiz_project_build(clean=false, projectName="main")`: 성공
- `curl -b "season-wiz-project=main; season-wiz-devmode=true" http://127.0.0.1:3000/cards`: 200 OK
- `curl -b "season-wiz-project=main; season-wiz-devmode=true" http://127.0.0.1:3000/users`: 200 OK
- 빌드 산출물 `project/main/build/dist/build/main.js`에서 `page.cards`, `page.users` 모두 `search-field:focus-within` 및 `search-field input:focus-visible` reset 규칙 반영 확인

## 남은 리스크

- 현재 컨테이너에 Playwright/Chromium/Chrome이 없어 실제 브라우저 focus 상태 스크린샷 검증은 수행하지 못했다.
