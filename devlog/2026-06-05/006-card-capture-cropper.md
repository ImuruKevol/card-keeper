# 006. 명함 촬영 이미지 전체화면 crop 전송 흐름 추가

## 사용자 원본 요청

- 리뷰 ID: `whcwfutxupufshwcvsauanrcrxqmehtl`
- 제목: OCR 로직 개편
- 요청 내용:
  - OCR 품질 저하를 막기 위해 촬영 사진 품질을 과도하게 낮추지 않는다.
  - 사용자가 촬영한 사진을 전체화면으로 보여준다.
  - 잘라낼 명함 영역을 표시하고, 사용자가 사진의 명함을 그 영역에 맞춘 뒤 잘라낸 부분만 서버로 전송해 업로드할 수 있게 한다.

## 변경 사항

- 사진 선택/촬영 후 바로 분석 슬롯에 저장하지 않고, 전체화면 cropper를 먼저 열도록 변경했다.
- 고정된 명함 프레임과 어두운 외부 마스크를 추가했다.
- 사용자가 사진을 드래그하고 확대 슬라이더 또는 휠로 배율을 조절해 명함을 프레임에 맞출 수 있게 했다.
- `적용` 시 프레임 영역만 원본 이미지 좌표로 crop한 뒤 JPEG로 변환해 슬롯 preview에 저장하도록 했다.
- crop 결과는 긴 변 최대 `2200px`, JPEG 품질 `0.92 → 0.88 → 0.84` 범위에서만 조정해 OCR 품질 저하를 줄였다.
- cropper 취소/초기화/적용 액션과 object URL 해제 처리를 추가했다.

## 변경 파일

- `src/app/page.cards/view.ts`
- `src/app/page.cards/view.pug`
- `src/app/page.cards/view.scss`
- `devlog.md`
- `devlog/2026-06-05/006-card-capture-cropper.md`

## 검증 결과

- `wiz_project_build(clean=false, projectName="main")`: 성공
- `npm run test:e2e -- --project=chromium`: 성공, 3 passed
- `PYTHONDONTWRITEBYTECODE=1 python tests/ocr_business_card_smoke.py`: 통과

## 남은 리스크

- 실제 모바일 PWA 카메라 화면에서 드래그/확대/crop 적용은 현재 환경에서 직접 재현하지 못했다.
- 브라우저가 원본 촬영 포맷을 디코딩하지 못하는 경우 cropper 진입 전에 이미지 읽기 실패 안내가 표시된다.
