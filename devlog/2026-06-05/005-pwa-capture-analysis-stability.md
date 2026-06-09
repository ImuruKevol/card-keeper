# 005. PWA 촬영 이미지 분석 전송 안정화

## 사용자 원본 요청

- 리뷰 ID: `whcwfutxupufshwcvsauanrcrxqmehtl`
- 제목: OCR 로직 개편
- 요청 내용:
  - PWA 앱을 통해 사진 분석 등록 모달에서 촬영 후 분석을 누르면 `서버 분석 실패`라고만 표시되는 문제를 해결한다.

## 변경 사항

- 촬영 이미지를 서버로 보내기 전에 브라우저에서 JPEG로 강제 변환하도록 변경했다.
- PWA 카메라 원본 이미지가 커서 `wiz.call` 요청 자체가 실패할 수 있는 상황을 줄이기 위해 이미지 크기와 JPEG 품질을 단계적으로 낮추도록 했다.
  - 긴 변 기준 `1800px → 1600px → 1400px → 1200px → 1000px`
  - JPEG 품질 `0.82 → 0.74 → 0.66 → 0.58 → 0.5`
  - 목표 전송 크기 약 `1.4MB` 이하
- 브라우저에서 촬영 이미지를 읽거나 변환하지 못하는 경우 원본을 그대로 보내지 않고 사용자에게 변환 실패 메시지를 표시하도록 했다.
- 서버 분석 호출 예외가 발생하면 가능한 실제 오류 메시지를 표시하도록 보강했다.

## 변경 파일

- `src/app/page.cards/view.ts`
- `devlog.md`
- `devlog/2026-06-05/005-pwa-capture-analysis-stability.md`

## 검증 결과

- `wiz_project_build(clean=false, projectName="main")`: 성공
- `npm run test:e2e -- --project=chromium`: 성공, 3 passed
- `PYTHONDONTWRITEBYTECODE=1 python tests/ocr_business_card_smoke.py`: 통과

## 남은 리스크

- 실제 모바일 PWA 카메라 권한/촬영 플로우는 현재 환경에서 직접 재현하지 못했다.
- 일부 기기가 브라우저에서 디코딩할 수 없는 이미지 포맷을 반환하면 분석 대신 변환 실패 안내가 표시된다.
