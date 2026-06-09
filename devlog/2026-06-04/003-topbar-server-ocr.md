# 003. 상단 앱바 전환 및 서버 우선 OCR 분석 흐름 보강

## 사용자 원 요청

```text
- 사용자 기준으로 메뉴는 하나만 있고, 관리자만 추가로 사용자 관리가 있는 구조이기 때문에 화면 레이아웃이 왼쪽 사이드바 형태로 되어있는게 레이아웃, 디자인적으로 맞지 않아.
- OCR은 JS로 하는게 성능이 좋을지, 아니면 python으로 서버에서 OCR 분석 후 저장을 하는게 나을지 OCR 품질, 성능 등을 종합적으로 분석해서 로직을 변경하거나 보완하던가 해줘.
```

## 변경 요약

- 인증 후 공통 레이아웃을 좌측 사이드바에서 상단 앱바 구조로 변경했다.
- 일반 사용자는 명함 관리만 보이고, 관리자에게만 상단 사용자 관리 버튼이 노출되도록 정리했다.
- 기존 사이드바 컴포넌트 앱을 삭제하고, 명함/사용자 화면의 헤더가 상단 앱바와 겹치지 않도록 sticky nav 구조를 제거했다.
- 명함 OCR은 서버 우선 구조로 변경했다. Python API가 Pillow 전처리 후 Tesseract `kor+eng` 분석을 시도하고, 서버 OCR 엔진이 없거나 실패하면 브라우저 Tesseract.js로 fallback한다.
- 클라이언트에서 업로드 이미지를 최대 1800px JPEG로 축소해 서버 전송 및 브라우저 OCR 비용을 줄였다.
- 분석 모달에 실제 분석 엔진(서버/브라우저/미가용)을 표시하도록 보강했다.
- README에 화면 구성, OCR 판단 근거, 서버 OCR 활성화 조건을 문서화했다.

## 변경 파일

- `README.md`
- `requirements.txt`
- `src/app/component.nav.sidebar/` 삭제
- `src/app/layout.sidebar/app.json`
- `src/app/layout.sidebar/view.pug`
- `src/app/layout.sidebar/view.ts`
- `src/app/page.cards/api.py`
- `src/app/page.cards/view.pug`
- `src/app/page.cards/view.ts`
- `src/app/page.users/view.pug`
- `devlog.md`
- `devlog/2026-06-04/003-topbar-server-ocr.md`

## 검증 결과

- `python3 -m py_compile project/main/src/app/page.cards/api.py`: 통과
- `wiz_project_build(clean=true, projectName="main")`: 성공
- `https://bus.sub.nanoha.kr/cards` + `season-wiz-project=main`, `season-wiz-devmode=true`: HTTP 200
- `https://bus.sub.nanoha.kr/users` + `season-wiz-project=main`, `season-wiz-devmode=true`: HTTP 200
- `https://bus.sub.nanoha.kr/wiz/api/page.cards/list` 비로그인 호출: HTTP 200, WIZ 본문 `code: 401` 확인
- `https://bus.sub.nanoha.kr/wiz/api/page.cards/analyze` 비로그인 호출: HTTP 200, WIZ 본문 `code: 401` 확인
- 로컬 런타임 확인: `PIL`은 설치되어 있으나 `tesseract` 바이너리와 `pytesseract` 런타임 패키지는 현재 미설치 상태다. 프로젝트 의존성에는 `pytesseract==0.3.13`을 추가했고, 서버 OCR 미가용 시 브라우저 OCR fallback이 실행되도록 구현했다.

## 남은 리스크

- 서버 OCR을 실제 운영에서 사용하려면 OS 패키지 `tesseract-ocr`, `tesseract-ocr-kor`, `tesseract-ocr-eng` 설치가 필요하다.
- 인증 세션이 없어 실제 이미지 업로드 후 서버 OCR 성공 경로는 API 수준에서 직접 실행하지 못했다.
