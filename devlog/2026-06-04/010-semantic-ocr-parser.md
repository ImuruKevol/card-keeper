# 010. 한글·영문 명함 OCR 의미 기반 필드 파싱 확장

## 사용자 원 요청

```text
OCR로 텍스트를 가져올 때 항상 같은 키워드로만 명함이 만들어지지는 않으니 비슷한 텍스트, 의미상 같은 단어들 등을 모두 처리할 수 있도록 고도화해줘.
한글, 영어 모두
```

## 변경 요약

- 서버 OCR 파서에서 한글/영문 이름 라벨(`성명`, `이름`, `Name`, `Contact`)을 인식하도록 보강했다.
- 회사명 추출을 법인 표기 중심에서 브랜드+업종형 표기까지 확장했다.
  - 예: `유한회사`, `재단법인`, `나노하테크 연구소`, `Labs`, `Studio`, `Partners`, `Technologies`, `Systems`
- 부서/직책 추출 어휘를 한글/영문 유사 표현으로 확장했다.
  - 예: `사업부`, `부문`, `파트`, `랩`, `연구소`, `Dept.`, `Division`, `Unit`, `Squad`, `Chapter`, `Product Manager`, `Principal`
- 연락처 추출을 단순 번호 순서가 아니라 의미 라벨 기준으로 보강했다.
  - `휴대폰/Mobile/Cell/CP/HP`는 휴대폰, `대표번호/Tel/Direct/Main`은 전화, `Fax/팩스`는 저장 대상에서 제외한다.
- 주소/웹사이트 파싱에서 `소재지`, `HQ`, `Office`, `Location`, `Address`, `Web`, `Homepage` 등 유사 표현을 처리하도록 보강했다.
- `Center/센터`처럼 주소와 부서명 양쪽에서 쓰이는 단어는 역할 추출에서 강한 주소 단서만 제외하도록 분리해 `Cloud AI Center` 같은 부서명이 누락되지 않게 했다.
- 국제 전화 표기에서 `+82 2 ...`처럼 지역번호가 한 자리로 인식되는 케이스도 전화번호로 처리하도록 보강했다.
- 이메일 도메인이 웹사이트로 잘못 들어가지 않도록 URL 후보에서 이메일 span 내부 매치를 제외했다.
- 브라우저 Tesseract.js fallback 파서도 서버 파서와 같은 의미군을 반영하도록 맞췄다.
- synthetic OCR 텍스트 테스트를 추가해 한글/영문 동의어 기반 필드 추출을 검증했다.

## 변경 파일

- `README.md`
- `src/app/page.cards/api.py`
- `src/app/page.cards/view.ts`
- `tests/ocr_business_card_smoke.py`
- `devlog.md`
- `devlog/2026-06-04/010-semantic-ocr-parser.md`

## 검증 결과

- `python -m py_compile src/app/page.cards/api.py tests/ocr_business_card_smoke.py`: 통과
- `python tests/ocr_business_card_smoke.py`: 통과
  - synthetic 한글 OCR: `성명`, `나노하테크 연구소`, `전략기획본부`, `수석매니저`, `휴대폰`, `대표번호`, `팩스`, `소재지`, `홈페이지` 처리 확인
  - synthetic 영문 OCR: `Name`, `Nanoha Labs LLC`, `Principal Product Manager`, `Growth Division`, `Cell`, `Direct`, `Fax`, `HQ`, `Web` 처리 확인
  - synthetic 영문 OCR: `Cloud AI Center`, `Office +82 2 ...`, `Website` 처리 확인
  - 실제 업로드 이미지 2장과 양면 병합 smoke도 기존과 동일하게 통과
- `wiz_project_build(clean=false, projectName="main")`: 성공
- `npx playwright test`: 데스크톱 Chromium 3개 + 모바일 Chromium 3개, 총 6개 smoke test 통과

## 남은 리스크

- 명함은 자유 형식이라 완전한 의미 이해가 필요한 예외는 정규식 기반 파서만으로 한계가 있다.
- 현장 OCR 결과가 계속 누락되거나 잘못 매핑되면 Vision OCR/LLM 기반 보정 fallback을 서버 측 비밀키로 추가하는 후속 작업이 필요하다.
