# 모바일 명함 등록 버튼 제거 및 촬영 아이콘 보정

- **ID**: 002
- **날짜**: 2026-06-12
- **유형**: UX 개선

## 작업 요약
모바일 폭에서 상단 `명함 등록` 버튼을 숨기고 import/CSV-XLSX 두 버튼만 2열로 보이도록 조정했다.
`명함 촬영` 버튼의 카메라 아이콘에 렌즈 원형 path를 추가해 카메라 형태가 명확히 보이도록 보정했다.

## 원문 요청사항
```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

- 모바일 기준으로 명함 등록 버튼 제거
- 명함 촬영 아이콘이 이상해졌음.

## 리뷰 요약

- 리뷰 ID: mafsqshttwkzwgjtxiwnbtbhylclhtsl
- 제목: PWA 모바일 UI 개선
- 요청 링크: https://bus.sub.nanoha.kr/
- Codex 요청자: 권태욱
- 프로젝트 루트: /opt/app
- Codex 세션 ID: 019eb737-d9ef-7953-b12b-4687ce54f666
- 스크린샷 컨텍스트: 없음
- 에이전트 작업 지시서 컨텍스트: 없음
- HTML 문서 생성 규칙 컨텍스트: 없음
- HTML 문서 설정 컨텍스트: 없음
- HTML 프로젝트 인스트럭션 파일: 없음
- 첨부파일 컨텍스트: 0개

## 세션 처리

저장된 Codex 세션을 resume해 이전 대화 맥락을 우선 사용하세요. 이전 Codex 히스토리는 이 요청에 포함되지 않습니다.
```

## 변경 파일 목록
- `src/app/page.cards/view.scss`
  - `max-width: 900px` 모바일 레이아웃에서 `.capture-toolbar-actions`를 2열로 변경
  - 모바일에서 `.capture-toolbar-actions .primary-action`을 숨김 처리
  - `max-width: 620px` 보정 규칙도 2열 유지로 정리
- `src/app/page.cards/view.pug`
  - `명함 촬영` 버튼의 카메라 SVG에 렌즈 path 추가
  - 등록 모달 내부 `명함 촬영` 버튼 아이콘도 동일하게 보정
- `devlog.md`
  - 이번 작업 요약 행 추가
- `devlog/2026-06-12/002-mobile-register-button-camera-icon.md`
  - 이번 작업 상세 기록 추가

## 검증 결과
- `wiz_project_build(clean=false)` 성공.
- `git diff --check -- src/app/page.cards/view.pug src/app/page.cards/view.scss` 통과.
- `curl -I -b "season-wiz-project=main; season-wiz-devmode=true" http://localhost:3000/cards` 200 OK 확인.
- 빌드 산출물에서 카메라 렌즈 path와 모바일 2열/숨김 CSS 반영 확인.
- Playwright 정적 렌더링으로 390px에서 `명함 등록` 버튼 `display: none`, 1024px에서 `display: flex` 확인.

## 비고
- 로컬 브라우저에서 실제 `/cards` 렌더링은 인증 세션이 없어 `/access`로 이동되므로 직접 확인하지 못했다.
