# 004. AI OCR 품질 판정 fallback 분리 및 명함 수정 UX 보강

## 사용자 원본 요청

- 리뷰 ID: `whcwfutxupufshwcvsauanrcrxqmehtl`
- 제목: OCR 로직 개편
- 요청 내용:
  - AI 실행 자체가 실패하는 경우엔 서버 python fallback이 맞는데, 품질 기준 미달에 대해서는 fallback 작동은 하지 않아야 한다.
  - AI 실행 결과는 무조건 JSON 포맷으로 통일한다.
  - 사용자가 등록한 명함 정보를 수정할 수 있게 한다.

## 변경 사항

- AI OCR 실행 실패와 품질 기준 미달을 분리했다.
  - Provider 호출 실패, 빈 응답, JSON 파싱 실패, 형식 불일치는 `execution_failed=True`, `available=False`로 반환해 서버 OCR fallback이 가능하게 유지했다.
  - 정상 JSON 응답이면 품질 기준 미달이어도 `available=True`, `quality_passed=False`로 반환해 서버 OCR fallback이 작동하지 않게 했다.
- AI OCR 결과 JSON 키를 통일했다.
  - `quality_passed`, `quality_score`, `payload_format`, `schema_name`, `execution_failed`를 추가했다.
  - 실패 응답도 `fields`, `quality_notes`, `payload_format=json`을 포함하도록 정리했다.
- 카드 분석 API 결과에 AI 품질 상태를 전달하고, 품질 미달 메시지를 `AI 분석 완료, 품질 확인 필요`로 표시하도록 보강했다.
- `list()` API 함수명이 Python 내장 `list`를 가려 AI 필드 병합의 `isinstance(value, list)`가 실패하던 잠재 오류를 수정했다.
- 명함 편집 UX를 보강했다.
  - 편집 모드 진입 시 서버 단건 조회로 최신 등록 정보를 가져온 뒤 폼에 채우도록 변경했다.
  - 편집 상태의 저장 버튼 문구를 `수정 저장`/`수정 중...`으로 표시하도록 변경했다.

## 변경 파일

- `src/model/struct/ai_setting.py`
- `src/app/page.cards/api.py`
- `src/app/page.cards/view.ts`
- `src/app/page.cards/view.pug`
- `devlog.md`
- `devlog/2026-06-05/003-ai-ocr-prompt-structured-output-catchup.md`
- `devlog/2026-06-05/004-ai-ocr-quality-json-edit.md`

## 검증 결과

- `PYTHONDONTWRITEBYTECODE=1 python -m py_compile src/model/struct/ai_setting.py src/app/page.cards/api.py`: 통과
- AI OCR normalizer 로컬 검증: 통과
  - 품질 미달 JSON 응답은 `available=True`, `quality_passed=False`, `execution_failed=False`
  - 실행 실패 응답은 `available=False`, `execution_failed=True`, `payload_format=json`
- 카드 분석 API fallback 게이트 모의 검증: 통과
  - 품질 미달 AI JSON 응답에서 서버 OCR 호출 없음
  - 응답 `fallback=False`, `quality_passed=False`, `payload_format=json`
- `PYTHONDONTWRITEBYTECODE=1 python tests/ocr_business_card_smoke.py`: 통과
- `wiz_project_build(clean=false, projectName="main")`: 성공
- `npm run test:e2e -- --project=chromium`: 성공, 3 passed

## 남은 리스크

- 실제 OpenAI, Google, Ollama Provider 호출은 API Key 및 로컬 Ollama 서버가 없어 수행하지 못했다.
- AI가 JSON은 반환하지만 실제 이미지 판독이 부정확한 경우에도 서버 OCR fallback은 의도적으로 작동하지 않으므로, 사용자가 수정 화면에서 보정해야 한다.
