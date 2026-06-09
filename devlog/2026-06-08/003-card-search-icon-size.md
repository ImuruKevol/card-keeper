# 명함 목록 검색 입력 돋보기 아이콘 크기 고정

- **ID**: 003
- **날짜**: 2026-06-08
- **리뷰 ID**: hfaceqgtvkzkyxfxbvbjwyxdfmrbxcuq
- **유형**: 버그 수정

## 작업 요약

명함 목록의 검색 입력 왼쪽 돋보기 SVG가 아이콘 크기 공통 규칙에 포함되지 않아 크게 렌더링될 수 있는 문제를 수정했다.
`.search-field svg`를 기존 18px 아이콘 크기 규칙에 포함하고, flex 컨테이너 안에서 입력 영역을 밀지 않도록 고정 flex 항목으로 지정했다.

## 원문 요청사항

```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

작업 시작

## 리뷰 요약

- 리뷰 ID: hfaceqgtvkzkyxfxbvbjwyxdfmrbxcuq
- 제목: 검색 input 버그
- 요청 링크: https://bus.sub.nanoha.kr/access
- Codex 요청자: 권태욱
- 프로젝트 루트: /opt/app

## 리뷰어 요청 내용

검색 input에 돋보기 아이콘이 대문짝만하게 들어가있음.
```

## 변경 파일 목록

- `src/app/page.cards/view.scss`
  - `.search-field svg`를 18px 아이콘 크기 규칙에 추가
  - `.search-field svg`에 `flex: 0 0 auto` 적용
- `devlog.md`
- `devlog/2026-06-08/003-card-search-icon-size.md`

## 검증 결과

- `wiz_project_build(clean=false, projectName="main")`: 성공
- `curl -b "season-wiz-project=main; season-wiz-devmode=true" http://127.0.0.1:3000/access`: 200 OK
- `curl -b "season-wiz-project=main; season-wiz-devmode=true" http://127.0.0.1:3000/cards`: 200 OK
- 빌드 산출물 `project/main/build/dist/build/main.js`에서 `page.cards` 스타일에 `.search-field svg`의 `width: 18px`, `height: 18px`, `flex: 0 0 auto` 규칙 포함 확인

## 남은 리스크

- 현재 컨테이너에 Playwright/Chromium/Chrome이 없어 브라우저 computed style 또는 스크린샷 검증은 수행하지 못했다.
