# 검색어 전용 초기화 버튼 및 상세 액션 아이콘화

- **ID**: 012
- **날짜**: 2026-06-08
- **리뷰 ID**: kivsqhpmuzfldxjyjomqgzrxcrwfklmr
- **유형**: UX 개선

## 작업 요약

검색어 또는 정렬 토글이 기본값이 아닐 때 필터 바 끝에 표시되던 전체 초기화 X 버튼을 제거했다.
검색어가 있을 때만 검색 input 오른쪽 내부에 작은 X 버튼을 표시하고, 이 버튼은 검색어만 초기화하도록 변경했다.
상세 모달의 `메일`, `전화`, `열기` 텍스트 링크 버튼을 이메일/전화/외부 링크 아이콘 버튼으로 교체했다.

## 원문 요청사항

```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

- input이나 기본값이 아닌 토글 버튼 선택 시 생기는 X 버튼을 삭제해줘. 대신 input값만 초기화할 수 있는 x 버튼을 input 오른쪽 끝에 absolute같은걸로 input 위에 덮어줘.
- 상세 모달의 메일, 전화, 열기 버튼을 텍스트로 표시하지 말고 아이콘 버튼으로 수정해줘.

## 리뷰 요약

- 리뷰 ID: kivsqhpmuzfldxjyjomqgzrxcrwfklmr
- 제목: UI 추가 개선
- 요청 링크: https://bus.sub.nanoha.kr/
- Codex 요청자: 권태욱
- 프로젝트 루트: /opt/app
```

## 변경 파일 목록

- `src/app/page.cards/view.pug`
  - 필터 바의 전체 초기화 버튼 제거
  - 검색 input 내부 검색어 초기화 버튼 추가
  - 상세 액션 링크를 텍스트에서 아이콘 SVG로 변경
- `src/app/page.cards/view.ts`
  - `clearSearchText()` 추가
  - 미사용 전체 필터 초기화 로직 제거
  - 상세 액션 필드에 `actionType` 추가
- `src/app/page.cards/view.scss`
  - input 내부 초기화 버튼 absolute 배치 스타일 추가
  - 상세 액션 버튼을 30px 아이콘 버튼으로 조정
- `devlog.md`
- `devlog/2026-06-08/012-search-clear-and-detail-action-icons.md`

## 검증 결과

- `wiz_project_build(clean=false, projectName="main")`: 성공
- `npm run test:e2e -- --project=chromium --project=mobile-chromium`: 성공, 개발 쿠키 `season-wiz-project=main`, `season-wiz-devmode=true` 적용된 smoke test 6개 통과

## 남은 리스크

- 실제 로그인 계정 비밀번호가 없어 `/cards` 로그인 후 검색어 초기화 버튼과 상세 모달 아이콘 버튼을 브라우저에서 직접 조작 검증하지 못했다.
