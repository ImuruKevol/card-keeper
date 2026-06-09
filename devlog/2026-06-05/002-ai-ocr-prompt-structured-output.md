# 002. AI OCR 프롬프트와 구조화 응답 검증 보강

## 사용자 원본 요청

- 리뷰 ID: `whcwfutxupufshwcvsauanrcrxqmehtl`
- 제목: OCR 로직 개편
- 요청 내용:
  - AI API로 OCR을 해서 필요한 정보를 추출할 때, 프롬프트를 잘 설정해서 현재 이 서비스에 필요한 정보를 확실하게 가져올 수 있도록 보정한다.
  - response 포맷 등을 지정하는 등 여러 보정/보완 수단을 추가한다.

## 변경 사항

- AI OCR 프롬프트를 명함 관리 서비스 전용으로 확장했다.
  - 이름, 회사, 부서, 직책, 이메일, 휴대폰, 전화, 주소, 웹사이트, 태그, 메모의 역할과 구분 기준을 명시했다.
  - Fax 제외, 이메일 도메인과 웹사이트 중복 금지, 추측 금지, 불확실 값은 빈 문자열 처리 규칙을 추가했다.
- 공통 JSON Schema를 추가하고 Provider별 구조화 응답 옵션에 연결했다.
  - OpenAI: `response_format.type=json_schema`, `strict=true` 우선 사용, 미지원 시 JSON mode fallback
  - Google: `responseMimeType=application/json`, `responseSchema` 우선 사용, 미지원 시 JSON mode fallback
  - Ollama: `format`에 JSON Schema 우선 전달, 미지원 시 `json` fallback
- AI 응답 파싱/정규화/검증을 보강했다.
  - 이메일, 전화번호, 웹사이트 형식 정규화
  - 휴대폰/대표전화 분리 보정
  - `AIOCR` 태그 자동 보강
  - 핵심 필드 품질 점수와 `quality_notes` 생성
  - 품질 기준 미달 시 AI OCR을 실패로 처리해 서버 Python OCR fallback이 작동하도록 변경
- AI OCR 재시도 로직을 추가했다.
  - 1차 응답이 파싱/검증에 실패하면 더 강한 재확인 프롬프트로 1회 재시도한다.
- 카드 분석 결과에 AI `quality_notes`와 `attempts`를 포함해 fallback 원인 추적을 보강했다.

## 변경 파일

- `src/model/struct/ai_setting.py`
- `src/app/page.cards/api.py`
- `devlog.md`
- `devlog/2026-06-05/002-ai-ocr-prompt-structured-output.md`

## 검증 결과

- `python -m py_compile src/model/struct/ai_setting.py src/app/page.cards/api.py`: 통과
- AI OCR schema/normalizer 로컬 검증: 통과
- `python tests/ocr_business_card_smoke.py`: 통과
- `wiz_project_build(clean=false, projectName="main")`: 성공
- `npm run test:e2e -- --project=chromium`: 성공, 3 passed

## 남은 리스크

- 실제 OpenAI, Google, Ollama 호출은 API Key 및 로컬 Ollama 서버가 없어 수행하지 못했다.
- Provider별 structured output 지원 범위가 모델/버전에 따라 다를 수 있어, 미지원 시 JSON mode로 fallback하도록 처리했다.
- AI 응답 검증 기준이 보수적으로 동작하면 일부 애매한 명함은 AI 결과 대신 서버 OCR fallback으로 저장될 수 있다.
