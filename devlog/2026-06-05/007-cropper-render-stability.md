# 007. cropper 상호작용 중 반복 렌더링 제거

## 사용자 원본 요청

- 리뷰 ID: `whcwfutxupufshwcvsauanrcrxqmehtl`
- 제목: OCR 로직 개편
- 요청 내용:
  - 크롭 화면에서 렌더링 로직상 문제가 있는지 UI가 계속 깜빡거리는 문제를 해결한다.

## 변경 사항

- cropper 초기화 직후 불필요한 추가 `service.render()` 호출을 제거했다.
- 확대 슬라이더와 휠 조작 중 반복 호출되던 `service.render()`를 제거했다.
- 이미지 transform을 Angular 템플릿 바인딩으로 계속 재계산하지 않고, `syncCropperPhoto()`에서 DOM style로 직접 반영하도록 변경했다.
- cropper 이미지의 초기 opacity를 숨기고, 크기/위치 계산이 끝난 뒤 표시해 원본 이미지가 자연 크기로 순간 노출되는 현상을 줄였다.
- 슬라이더 min/max/value와 배율 표시도 DOM에 직접 동기화하도록 보강했다.

## 변경 파일

- `src/app/page.cards/view.ts`
- `src/app/page.cards/view.pug`
- `src/app/page.cards/view.scss`
- `devlog.md`
- `devlog/2026-06-05/007-cropper-render-stability.md`

## 검증 결과

- `wiz_project_build(clean=false, projectName="main")`: 성공
- `npm run test:e2e -- --project=chromium`: 성공, 3 passed
- `PYTHONDONTWRITEBYTECODE=1 python tests/ocr_business_card_smoke.py`: 통과

## 남은 리스크

- 실제 모바일 PWA 카메라 cropper 조작 중 깜빡임은 현재 환경에서 직접 재현해 확인하지 못했다.
