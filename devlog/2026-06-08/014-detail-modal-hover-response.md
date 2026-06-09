# 상세 모달 버튼 hover 반응 지연 제거

## 원 요청

- 리뷰 ID: `adqztmcatlcofimfbsttklnumjohluaf`
- 제목: 상세 모달 UI 수정
- 요청: "각 버튼들에 마우스 hover 시 hover 스타일이 적용은 되는 것 같은데 계속 한템포 느리게 적용되어서 묘하게 불쾌해."

## 변경 파일

- `src/app/page.cards/view.scss`
- `devlog.md`
- `devlog/2026-06-08/014-detail-modal-hover-response.md`

## 변경 내용

- 명함 상세 모달 내부의 편집, 저장, 닫기, 연락 액션, 복사 버튼 hover 상태가 즉시 반응하도록 transition을 제거했다.
- 상세 모달에만 범위를 제한해 목록, 등록 모달, 페이지네이션 등 다른 화면의 버튼 전환감은 유지했다.

## 확인 결과

- `wiz_project_build(clean=false)` 성공.
- `npx playwright test tests/e2e/smoke.spec.ts --project=chromium` 성공.
- 검증 시 `season-wiz-project=main`, `season-wiz-devmode=true` 쿠키가 적용된 Playwright 설정을 사용했다.
