# 명함 목록 10개 단위 페이지네이션 추가

- 작업 ID: 013
- 날짜: 2026-06-08
- 리뷰 ID: kivsqhpmuzfldxjyjomqgzrxcrwfklmr
- 분류: UX

## 원 요청

현재 페이지네이션은 없는데 api response에는 페이지네이션이 적용되어있어.
맨 앞/뒤로, 이전/다음, 10페이지 단위 페이지네이션 컴포넌트를 추가해줘. 한 페이지 dump는 10개씩으로.

## 변경 요약

- 명함 목록 기본 요청 단위를 10개로 변경했다.
- API와 명함 모델의 기본 dump 값을 10으로 맞췄다.
- 목록 하단에 맨 앞/이전/10페이지 단위 번호/다음/맨 뒤 이동 버튼을 추가했다.
- 현재 페이지와 전체 페이지를 표시하고, 모바일에서는 페이지네이션 컨트롤이 가운데 정렬되도록 반응형 스타일을 추가했다.

## 변경 파일

- `src/app/page.cards/view.ts`
- `src/app/page.cards/view.pug`
- `src/app/page.cards/view.scss`
- `src/app/page.cards/api.py`
- `src/model/struct/business_card.py`
- `devlog.md`
- `devlog/2026-06-08/013-card-list-pagination.md`

## 검증

- `wiz_project_build(projectName="main", clean=false)` 통과
- `python -m py_compile src/app/page.cards/api.py src/model/struct/business_card.py` 통과
- `npm run test:e2e -- --project=chromium --project=mobile-chromium` 통과, 6 passed

## 남은 리스크

- 인증된 실제 명함 데이터 화면에서 다수 페이지 이동까지는 자동 테스트로 검증하지 못했다.
- 명함 총 개수가 10개 이하인 경우 페이지네이션은 의도대로 숨겨진다.
