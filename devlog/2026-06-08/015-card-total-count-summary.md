# 검색 시 총 명함 수 표시 고정

## 사용자 요청

pc 버전에서 검색 시 총 명함 숫자가 바뀜. 총 명함 수는 무조건 등록된 모든 명함 숫자가 표시되어야 함. 그리고 그 옆에 페이지, 표시 숫자들은 필요 없으니 삭제할 것. 그에 따라 디자인도 알맞게 수정할 것.

## 변경 파일

- `src/model/struct/business_card.py`
- `src/app/page.cards/api.py`
- `src/app/page.cards/view.ts`
- `src/app/page.cards/view.pug`
- `src/app/page.cards/view.scss`
- `devlog.md`
- `devlog/2026-06-08/015-card-total-count-summary.md`

## 작업 내용

- 명함 검색 결과 수와 전체 등록 명함 수를 분리해 API가 `all_total`을 함께 반환하도록 수정했다.
- PC 상단 제목과 등록 패널의 `총 명함` 숫자는 검색 조건과 무관하게 전체 등록 수를 표시하도록 변경했다.
- 등록 패널의 `페이지`, `표시` 숫자 셀을 제거하고 단일 총계 영역에 맞춰 그리드 폭과 정렬을 조정했다.
- 기존 `total` 값은 페이지네이션 계산용 검색 결과 수로 유지했다.

## 확인 결과

- `wiz_project_build(projectName="main", clean=false)` 성공
- `npm run test:e2e -- --project=chromium` 성공: 3 passed
- `python -m py_compile src/app/page.cards/api.py src/model/struct/business_card.py` 성공
