# 012. AI Provider 설정 및 AI 우선 OCR fallback 흐름 추가

## 사용자 원 요청

```text
작업 시작

리뷰 ID: whcwfutxupufshwcvsauanrcrxqmehtl
제목: OCR 로직 개편

- 관리자 메뉴에 AI Provider(구글, OpenAI, Ollama)를 선택할 수 있는 기능 추가
- AI Provider 선택 및 API Key 입력 시 사용할 수 있는 모델 목록을 불러오는 기능 추가 (이 때 OCR이 가능한 모델만 리스팅해야함)
- AI 모델 및 API Key가 등록이 되어있으면 기본적으로 AI를 이용해 명함을 인식해서 등록하도록 하고, fallback은 서버의 python으로 할 것.
```

## 변경 요약

- 관리자 전용 `/ai-settings` 페이지를 추가해 OpenAI, Google, Ollama Provider 선택, API Key 입력, Ollama Base URL 입력, 모델 목록 조회, 모델 저장을 지원했다.
- AI 설정 저장용 `ai_setting` DB 모델과 struct를 추가하고, API Key는 조회 응답에서 반환하지 않고 `has_api_key`만 반환하도록 구성했다.
- Provider별 모델 목록 조회를 추가했다.
  - OpenAI: `/v1/models` 응답에서 GPT vision 계열 및 vision 이름 패턴만 후보로 필터링
  - Google: Generative Language 모델 중 `generateContent` 지원 Gemini vision/multimodal 계열만 후보로 필터링
  - Ollama: `/api/tags`와 `/api/show`의 vision capability 또는 vision 모델명 힌트로 후보 필터링
- 명함 이미지 분석 API에서 AI 설정이 활성화되어 있으면 AI OCR을 먼저 시도하고, AI 실패/미설정 시 기존 서버 Python Tesseract OCR로 fallback하도록 변경했다.
- AI 분석 결과는 JSON 필드로 정규화하고, 누락 필드는 기존 서버 OCR 파서로 보완할 수 있게 했다.
- 명함 화면의 분석 상태, 엔진 표시, 저장 source 라벨에 `AI 분석` 흐름을 추가했다.
- 상단 관리자 메뉴에 `AI OCR 설정` 링크와 ko/en 번역 키를 추가했다.

## 변경 파일

- `src/model/db/ai_setting.py`
- `src/model/struct/ai_setting.py`
- `src/model/struct.py`
- `src/app/page.ai_settings/app.json`
- `src/app/page.ai_settings/api.py`
- `src/app/page.ai_settings/view.pug`
- `src/app/page.ai_settings/view.scss`
- `src/app/page.ai_settings/view.ts`
- `src/app/layout.sidebar/view.pug`
- `src/app/page.cards/api.py`
- `src/app/page.cards/view.ts`
- `src/assets/lang/ko.json`
- `src/assets/lang/en.json`
- `devlog.md`
- `devlog/2026-06-04/012-ai-provider-ocr-settings.md`

## 검증 결과

- `python -m py_compile src/model/db/ai_setting.py src/model/struct/ai_setting.py src/model/struct.py src/app/page.ai_settings/api.py src/app/page.cards/api.py`: 통과
- `wiz_project_build(clean=false, projectName="main")`: 성공
- `python tests/ocr_business_card_smoke.py`: 통과
  - 기존 서버 OCR 앞면/뒷면 인식, 양면 병합, 회전 보정 스모크 유지 확인
- Provider 모델 필터 로컬 확인: OpenAI/Google/Ollama OCR 후보 필터 기본 케이스 통과
- `npm run test:e2e`: 데스크톱 Chromium 3개 + 모바일 Chromium 3개, 총 6개 smoke test 통과
- Playwright 단건 확인: WIZ 쿠키 `season-wiz-project=main`, `season-wiz-devmode=true` 적용 후 `/ai-settings` 비로그인 접근이 `/access`로 이동함을 확인

## 남은 리스크

- 실제 OpenAI, Google, Ollama 모델 목록 조회와 AI OCR 호출은 운영 API Key/로컬 Ollama 서버가 필요해 이번 검증에서는 외부 Provider 실호출까지 수행하지 않았다.
- OpenAI와 Google의 모델 목록 API가 capability metadata를 충분히 제공하지 않는 경우가 있어, OCR 가능 모델 필터는 모델명/지원 메서드 기반 보수적 필터를 함께 사용한다.
- API Key는 조회 응답에는 노출하지 않지만 DB에는 애플리케이션이 호출할 수 있는 형태로 저장된다. 운영 보안 수준에 따라 별도 secret store 또는 암호화 저장이 필요할 수 있다.
- AI OCR 응답이 JSON 형식을 지키지 않거나 모델이 이미지 입력을 거부하면 해당 면은 서버 Python OCR로 fallback한다.
