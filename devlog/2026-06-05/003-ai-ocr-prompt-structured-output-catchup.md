# 003. AI OCR 프롬프트와 구조화 응답 검증 보강 catch-up

## 사용자 원본 요청

- 리뷰 ID: `whcwfutxupufshwcvsauanrcrxqmehtl`
- 제목: OCR 로직 개편
- 요청 내용:
  - AI API로 OCR을 해서 필요한 정보를 추출할 때, 프롬프트를 잘 설정해서 현재 이 서비스에 필요한 정보를 확실하게 가져올 수 있도록 보정한다.
  - response 포맷 등을 지정하는 등 여러 보정/보완 수단을 추가한다.

## 변경 파일

- `src/model/struct/ai_setting.py`
- `src/app/page.cards/api.py`
- `devlog/2026-06-05/002-ai-ocr-prompt-structured-output.md`

## 검증 결과

- 이전 작업에서 `python -m py_compile src/model/struct/ai_setting.py src/app/page.cards/api.py` 통과를 확인했다.
- 이전 작업에서 AI OCR schema/normalizer 로컬 검증 통과를 확인했다.
- 이전 작업에서 `python tests/ocr_business_card_smoke.py` 통과를 확인했다.
- 이전 작업에서 `wiz_project_build(clean=false, projectName="main")` 성공을 확인했다.
- 이전 작업에서 `npm run test:e2e -- --project=chromium` 성공을 확인했다.

## 비고

- 같은 리뷰 세션의 직전 작업 상세 파일은 작성되어 있었지만 `devlog.md` 요약 표에 연결되지 않아 catch-up 항목으로 보완했다.
