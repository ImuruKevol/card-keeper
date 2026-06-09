# 009. 명함 앞면·뒷면 연속 등록 및 OCR 성능 패스 최적화

## 사용자 원 요청

```text
- 앞면, 뒷면을 연속으로 찍어서 등록할 수 있도록 개선해줘.
- 성능이 생각보다 되게 별로야. 개선할 방법을 찾아줘. 끝까지 성능 개선이 되지 않으면 그냥 gemini api같은걸로 ocr 로직을 돌려야 할 것 같아.
```

## 변경 요약

- 명함 등록 모달을 단일 이미지에서 `앞면`/`뒷면` 슬롯 구조로 변경했다.
- 사진 선택 후 즉시 OCR을 실행하지 않고, 앞면 촬영 후 뒷면 슬롯으로 넘어가며 사용자가 분석 버튼을 누를 때 촬영된 면을 일괄 분석하도록 변경했다.
- `page.cards/analyze` API가 기존 단일 이미지와 신규 `front_image`/`back_image` 요청을 모두 처리하도록 확장했다.
- 양면 분석 결과는 앞면 값을 우선하고 비어 있는 필드만 뒷면 결과로 보완하도록 병합했다.
- 서버 OCR은 `psm 11`을 먼저 실행하고 필수 필드가 부족할 때만 `psm 6`을 추가 실행하도록 변경했다.
- Tesseract confidence 계산용 `image_to_data` 추가 OCR 패스를 제거하고 필드 완성도 기반 confidence 추정으로 대체했다.
- 실제 업로드 이미지 기준 양면 OCR 총 패스 수가 4패스+confidence 추가 패스에서 3패스로 줄어들도록 smoke 테스트를 보강했다.
- 클라이언트 이미지 정규화 크기를 실제 앞면 이름 인식률에 맞춰 2200px JPEG로 조정했다.
- README에 양면 일괄 분석, 적응형 OCR 패스, Gemini류 Vision OCR fallback 검토 조건을 문서화했다.

## 변경 파일

- `README.md`
- `src/app/page.cards/api.py`
- `src/app/page.cards/view.pug`
- `src/app/page.cards/view.scss`
- `src/app/page.cards/view.ts`
- `tests/ocr_business_card_smoke.py`
- `devlog.md`
- `devlog/2026-06-04/008-dual-side-ocr-performance.md`

## 검증 결과

- `python -m py_compile src/app/page.cards/api.py tests/ocr_business_card_smoke.py`: 통과
- `python tests/ocr_business_card_smoke.py`: 통과
  - 앞면 OCR 2패스, 뒷면 OCR 1패스, 양면 병합 총 3패스 확인
  - 병합 결과: `권태욱`, `주식회사 시즌`, `개발팀`, `팀장`, `010-8378-3636`, `044-862-9307`, `한누리대로 219`, `www.season.co.kr`, `양면` 태그 확인
- `wiz_project_build(clean=false, projectName="main")`: 성공
- `npx playwright test`: 데스크톱 Chromium 3개 + 모바일 Chromium 3개, 총 6개 smoke test 통과

## 남은 리스크

- 로그인 계정 정보가 없어 실제 인증 세션에서 카메라/파일 입력 후 저장까지의 완전한 end-to-end 흐름은 직접 검증하지 못했다.
- Tesseract는 이미지 품질과 촬영 각도에 민감하므로, 현장 이미지가 계속 느리거나 부정확하면 Gemini 같은 Vision OCR API를 서버 비밀키 기반 fallback으로 추가하는 후속 작업이 필요하다.
