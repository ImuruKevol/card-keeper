# 명함 목록 이름·회사 통합 검색 적용

- **ID**: 010
- **날짜**: 2026-06-08
- **리뷰 ID**: kivsqhpmuzfldxjyjomqgzrxcrwfklmr
- **유형**: UX 개선 / 기능 개선

## 작업 요약

명함 목록 필터 바에서 `이름/회사` 검색 범위 토글을 제거했다.
검색 입력 기본 동작을 이름과 회사명을 동시에 검색하도록 변경했다.
검색 placeholder를 `이름 또는 회사명 검색`으로 바꿔 현재 동작과 맞췄다.

## 원문 요청사항

```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

이름/회사로 검색 토글 버튼을 제거하고 그냥 기본적으로 검색 시 이름과 회사 모두 검색이 되도록 해줘.

## 리뷰 요약

- 리뷰 ID: kivsqhpmuzfldxjyjomqgzrxcrwfklmr
- 제목: UI 추가 개선
- 요청 링크: https://bus.sub.nanoha.kr/
- Codex 요청자: 권태욱
- 프로젝트 루트: /opt/app
```

## 변경 파일 목록

- `src/app/page.cards/view.ts`
  - 검색 상태에서 `scope` 제거
  - 검색 placeholder를 이름/회사 통합 문구로 변경
  - 검색 범위 토글 헬퍼 제거
  - 필터 활성/초기화 기준에서 검색 범위 제외
- `src/app/page.cards/view.pug`
  - `이름/회사` 검색 범위 토글 제거
- `src/app/page.cards/api.py`
  - 목록 API 기본 검색 범위를 `name_company`로 변경
- `src/model/struct/business_card.py`
  - 기본 검색 범위를 `name_company`로 변경
  - `name_company` 범위에서 `name`, `company` 필드를 함께 조회
- `devlog.md`
- `devlog/2026-06-08/010-card-list-name-company-search.md`

## 검증 결과

- `wiz_project_build(clean=false, projectName="main")`: 성공
- `python -m py_compile src/app/page.cards/api.py src/model/struct/business_card.py`: 성공
- `npm run test:e2e -- --project=chromium --project=mobile-chromium`: 성공, 개발 쿠키 `season-wiz-project=main`, `season-wiz-devmode=true` 적용된 smoke test 6개 통과

## 남은 리스크

- 실제 로그인 계정 비밀번호가 없어 `/cards` 로그인 후 이름/회사 통합 검색 결과를 브라우저에서 직접 확인하지 못했다.
