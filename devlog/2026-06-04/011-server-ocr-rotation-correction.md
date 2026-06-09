# 011. 서버 OCR 고정 및 회전 촬영 이미지 자동 보정

## 사용자 원 요청

```text
계속 브라우저 분석이라고 뜨는데 JS로 하는 중인거야?
서버에서 python으로 분석하는게 훨씬 신뢰도가 높을 것 같아.
그리고 사진을 찍을 때 가로로 돌려서 찍는 경우가 있어. 분석하기 전에 이에 대한 부분을 보정할 수 있도록 해줘.
```

## 변경 요약

- 명함 분석 화면에서 서버 OCR 실패 시 자동으로 브라우저 Tesseract.js 분석으로 넘어가던 흐름을 중단했다.
- 서버 OCR 실패 시 `브라우저 분석 중` 대신 서버 OCR 실패 사유를 화면에 표시하도록 변경했다.
- 기존 저장 데이터가 `photo-browser` source를 갖고 있어도 목록에는 `브라우저 분석` 대신 `사진 분석`으로 표시되게 변경했다.
- 서버 Python OCR에서 원본 방향 분석이 부족할 때만 90도, 270도, 180도 회전 후보를 추가 분석하고, 필드 점수가 가장 높은 방향을 선택하도록 보강했다.
- 회전 보정이 실행된 경우 분석 엔진 표시에 `회전보정 {각도}도`를 함께 보여주도록 했다.
- 개별 Tesseract 호출 timeout을 25초로 조정하고, 특정 후보 timeout이 전체 분석 실패로 번지지 않도록 부분 실패를 격리했다.
- 휴대폰처럼 보이는 번호가 대표전화 필드에 중복/오인식되지 않도록 전화번호 분류 규칙을 보정했다.
- README의 OCR 전략을 서버 OCR 기준, 자동 브라우저 fallback 비활성 기준, 회전 후보 분석 기준으로 갱신했다.
- 실제 업로드 명함 이미지를 90도로 회전한 smoke 테스트를 추가했다.

## 변경 파일

- `README.md`
- `src/app/page.cards/api.py`
- `src/app/page.cards/view.ts`
- `tests/ocr_business_card_smoke.py`
- `devlog.md`
- `devlog/2026-06-04/011-server-ocr-rotation-correction.md`

## 검증 결과

- `python -m py_compile src/app/page.cards/api.py tests/ocr_business_card_smoke.py`: 통과
- `python tests/ocr_business_card_smoke.py`: 통과
  - 기존 실제 앞면/뒷면 OCR과 양면 병합 통과
  - 정상 양면 분석은 기존 기준처럼 총 3패스 유지
  - 90도 회전한 실제 앞면 이미지에서 서버가 `rotation=270`, `passes=5`를 선택하고 이름, 회사, 이메일, 휴대폰, 대표전화 복구 확인
- `wiz_project_build(clean=false, projectName="main")`: 성공
- `npx playwright test`: 데스크톱 Chromium 3개 + 모바일 Chromium 3개, 총 6개 smoke test 통과

## 남은 리스크

- 서버 OCR이 실제 배포 환경에서 동작하려면 `tesseract-ocr`, `tesseract-ocr-kor`, `tesseract-ocr-eng`, `pytesseract`가 운영 컨테이너/호스트에 설치되어 있어야 한다.
- 회전 보정은 원본 OCR 품질이 낮을 때만 추가 후보를 검사한다. 매우 흐리거나 일부만 잘린 사진은 회전 후보를 골라도 OCR 품질이 부족할 수 있다.
- 회전 보정이 필요한 이미지는 최대 5패스까지 실행될 수 있어 정상 방향 이미지보다 분석 시간이 늘어난다.
