# 명함 목록 최신순 정렬 제거 및 이름순 기본화

- **ID**: 009
- **날짜**: 2026-06-08
- **리뷰 ID**: kivsqhpmuzfldxjyjomqgzrxcrwfklmr
- **유형**: UX 개선 / 기능 개선

## 작업 요약

명함 목록 정렬 기준에서 `최신순` 토글을 제거했다.
초기 목록 조회와 필터 초기화 기준을 `이름순` + `오름차순`으로 변경했다.
목록 API와 명함 모델 검색 기본값도 이름순 오름차순으로 맞췄다.
기존 `recent()` 내부 헬퍼는 이름순 기본값 변경의 영향을 받지 않도록 직접 `updated DESC`로 조회하게 유지했다.

## 원문 요청사항

```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

최신순 정렬은 필요 없으니 제거하고, 이름순 정렬을 기본으로 해줘.

## 리뷰 요약

- 리뷰 ID: kivsqhpmuzfldxjyjomqgzrxcrwfklmr
- 제목: UI 추가 개선
- 요청 링크: https://bus.sub.nanoha.kr/
- Codex 요청자: 권태욱
- 프로젝트 루트: /opt/app
```

## 변경 파일 목록

- `src/app/page.cards/view.ts`
  - 목록 검색 상태 기본 정렬을 `sort: name`, `direction: asc`로 변경
  - 필터 초기화 기준을 이름순 오름차순으로 변경
- `src/app/page.cards/view.pug`
  - 정렬 기준 토글에서 `최신순` 버튼 제거
- `src/app/page.cards/view.scss`
  - 두 버튼 정렬 토글에 맞춰 모바일 `sort-mode` flex 기준 축소
- `src/app/page.cards/api.py`
  - 목록 API 정렬 기본값을 `name`/`asc`로 변경
- `src/model/struct/business_card.py`
  - 검색 기본 정렬을 이름순 오름차순으로 변경
  - 목록 검색 정렬 기준에서 `recent` 제거
  - `recent()` 헬퍼는 updated 내림차순 직접 조회로 유지
- `devlog.md`
- `devlog/2026-06-08/009-card-list-default-name-sort.md`

## 검증 결과

- `wiz_project_build(clean=false, projectName="main")`: 성공
- `python -m py_compile src/app/page.cards/api.py src/model/struct/business_card.py`: 성공
- `npm run test:e2e -- --project=chromium --project=mobile-chromium`: 성공, 개발 쿠키 `season-wiz-project=main`, `season-wiz-devmode=true` 적용된 smoke test 6개 통과

## 남은 리스크

- 실제 로그인 계정 비밀번호가 없어 `/cards` 로그인 후 명함 목록의 기본 정렬 상태를 브라우저에서 직접 확인하지 못했다.
