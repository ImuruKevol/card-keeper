# 007. 업로드 명함 사진 기반 OCR 파싱 및 서버 검증 강화

## 사용자 원 요청

```text
작업 시작

리뷰 ID: cmhacjkigavkijsklpibvbtaxeveqhsy
제목: OCR 기능 개발 및 테스트

현재 wiz root에 data 디렉토리에 내 옛날 명함의 앞면, 뒷면 사진을 찍어서 업로드해놨어.
이 사진들을 기준으로 OCR 기능 개발 및 검증, 테스트를 진행해줘.
```

## 변경 요약

- 서버 OCR에서 EXIF 방향 보정 후 `psm 11`과 `psm 6` 결과를 병합해 실제 명함 사진의 이름, 회사, 부서, 직책, 연락처, 주소, 웹사이트 추출률을 높였다.
- 한국어 앞면 사진의 `권 태 욱`, `개발팀 / 팀장`, `Addr.` 주소 형태와 영어 뒷면 사진의 `Taewook Kwon Lead of Software Development Team` 형태를 필드로 파싱하도록 정리했다.
- 브라우저 OCR fallback 파서도 같은 핵심 필드 추출 규칙을 반영하고, 주소 필드 자동 채움을 추가했다.
- `data/`의 업로드 이미지 2장을 직접 읽어 서버 OCR 결과를 assert하는 smoke 테스트를 추가했다.
- 서버 OCR 검증을 위해 런타임에 `pytesseract==0.3.13`, OS 패키지 `tesseract-ocr`, `tesseract-ocr-eng`, `tesseract-ocr-kor`를 설치했다.

## 변경 파일

- `README.md`
- `src/app/page.cards/api.py`
- `src/app/page.cards/view.ts`
- `tests/ocr_business_card_smoke.py`
- `devlog.md`
- `devlog/2026-06-04/007-business-card-ocr-validation.md`

## 검증 결과

- `python -m py_compile src/app/page.cards/api.py tests/ocr_business_card_smoke.py`: 통과
- `python tests/ocr_business_card_smoke.py`: 통과
  - 앞면: `권태욱`, `주식회사 시즌`, `개발팀`, `팀장`, `010-8378-3636`, `044-862-9307`, `한누리대로 219`, `www.season.co.kr` 추출 확인
  - 뒷면: `Taewook Kwon`, `Season Co. Ltd`, `Software Development Team`, `Lead`, `+82-10-8378-3636`, `+82-44-862-9307`, `Republic of Korea`, `www.season.co.kr` 추출 확인
- `wiz_project_build(clean=false, projectName="main")`: 성공
- `npx playwright test`: 데스크톱 Chromium 3개 + 모바일 Chromium 3개, 총 6개 smoke test 통과

## 남은 리스크

- 실제 로그인 세션 정보가 없어 인증 후 `/cards` 화면에서 파일 업로드를 통한 end-to-end 저장 흐름은 직접 검증하지 못했다.
- 서버 OCR은 OS 패키지 설치가 필요하므로, 운영/재배포 환경에서도 `tesseract-ocr`, `tesseract-ocr-kor`, `tesseract-ocr-eng`가 유지되어야 한다.
