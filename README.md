# 명함장

WIZ 프레임워크 기반 개인용 명함 관리 PWA입니다. 핵심 화면은 사진 업로드/카메라 촬영 후 분석 결과를 검토해 저장하는 명함 관리 화면이며, 관리자에게만 사용자 관리 화면을 노출합니다.

## 화면 구성

- 일반 사용자는 `/cards` 명함 관리 화면만 사용합니다.
- 관리자는 상단 앱바의 사용자 관리 버튼으로 `/users`에 이동합니다.
- 좌측 사이드바 없이 상단 앱바와 단일 작업 화면 중심으로 구성했습니다.

## 초기 접속 흐름

1. `/access`에서 로그인하거나 가입 신청을 합니다.
2. 첫 번째 가입 계정은 초기 부트스트랩을 위해 `admin / active` 상태로 생성됩니다.
3. 이후 가입 계정은 `user / pending` 상태로 생성되며, 관리자가 `/users`에서 승인해야 로그인할 수 있습니다.
4. 로그인 후 기본 진입점과 PWA 시작 URL은 `/cards`입니다.

## DB 설정

`config/database.py`는 아래 환경 변수를 읽어 MariaDB/MySQL에 연결합니다. WIZ ORM은 MariaDB도 `mysql` 드라이버 경로를 사용합니다.

| 환경 변수 | 기본값 |
|----------|--------|
| `DB_TYPE` | `mariadb` |
| `DB_HOST` | `db` |
| `DB_PORT` | `3306` |
| `DB_NAME` | `wiz` |
| `DB_USER` | `wiz` |
| `DB_PASSWORD` | `business@2026` |

## 주요 구조

```text
src/
├── app/
│   ├── page.access/        # 로그인 / 가입 신청
│   ├── page.cards/         # 명함 사진 분석 / 검색 / 수정
│   ├── page.users/         # 관리자 전용 사용자 승인/관리
│   └── layout.sidebar/     # 인증 후 상단 앱바 공통 레이아웃
├── route/
│   └── manifest/           # /manifest.json PWA manifest
├── controller/
│   ├── base.py             # 세션 초기화 및 요청 파싱
│   ├── user.py             # 로그인/활성 상태/세션 토큰 검증
│   └── admin.py            # 관리자 권한 검증
└── model/
    ├── db/
    │   ├── user.py
    │   ├── user_session.py
    │   ├── access_log.py
    │   └── business_card.py
    └── struct/
        ├── user.py
        ├── user_session.py
        ├── access_log.py
        └── business_card.py
```

## PWA 구성

- `config/season.py`: PWA 이름, 시작 URL, 테마 색상, 아이콘 경로 설정
- `src/route/manifest/`: `/manifest.json` 동적 응답
- `config/pwa/sw.js`: `/sw.js`에서 제공되는 서비스워커 캐시 정책
- `src/assets/brand/`: PWA 아이콘과 로고 자산

## 브라우저 테스트

Playwright Chromium smoke test를 프로젝트 루트에서 실행할 수 있습니다. 기본 프로젝트는 데스크톱 Chrome과 Pixel 7 모바일 Chrome입니다.

```bash
npm run playwright:install-deps
npm run playwright:install
npm run test:e2e
```

기본 테스트 대상은 `https://bus.sub.nanoha.kr`이며, 다른 환경은 `PLAYWRIGHT_BASE_URL`로 지정합니다. 테스트는 `season-wiz-project=main`, `season-wiz-devmode=true` 쿠키를 주입합니다.

## OCR 분석 전략

명함 OCR은 Python 서버 분석을 기준으로 동작합니다. Python 서버 분석은 기기 성능에 덜 흔들리고 이미지 전처리와 언어 설정을 통제하기 쉬워 품질과 응답 일관성이 더 좋습니다.

- `POST /wiz/api/page.cards/analyze`: 서버에서 EXIF 방향 보정과 Pillow 전처리 후 Tesseract `kor+eng` OCR을 시도합니다.
- 앞면/뒷면 이미지를 한 번에 보내면 앞면 필드를 우선하고 빈 값만 뒷면 결과로 보완합니다.
- 서버 OCR은 명함 사진에 강한 `psm 11` 결과를 기준으로 삼고, 필수 필드가 부족할 때만 `psm 6` 보완 패스를 실행합니다.
- 파서는 한글/영문 명함의 `성명/Name/Contact`, `휴대폰/Mobile/Cell`, `대표번호/Tel/Office/Direct`, `홈페이지/Web/Homepage`, `소재지/Address/HQ`, 법인명·회사 접미사, 부서·직책 유사 표현을 의미군으로 매핑합니다.
- 원본 OCR 결과가 부족하면 서버에서 90도, 270도, 180도 회전 후보를 추가 분석해 가장 점수가 높은 방향을 선택합니다.
- 서버에 `tesseract` 바이너리 또는 `pytesseract` 래퍼가 없으면 브라우저 OCR로 자동 전환하지 않고 서버 OCR 실패 원인을 화면에 표시합니다.
- 클라이언트는 OCR 품질 유지를 위해 업로드 이미지를 최대 2200px JPEG로 정규화하고, 촬영 직후가 아니라 사용자가 분석을 실행할 때 일괄 전송합니다.
- 서버 OCR을 실제 활성화하려면 Python 의존성 외에 운영 서버 OS 패키지 `tesseract-ocr`, `tesseract-ocr-kor`, `tesseract-ocr-eng` 설치가 필요합니다.
- Tesseract 결과가 계속 부족하면 `GEMINI_API_KEY` 같은 서버 측 비밀키 기반 Vision OCR fallback을 별도 서버 경로로 붙이는 것이 다음 선택지입니다.
- 업로드 검증 이미지 smoke 테스트는 `python tests/ocr_business_card_smoke.py`로 실행합니다.

## 주요 API

### 인증
- `POST /wiz/api/page.access/login` - 로그인
- `POST /wiz/api/page.access/signup` - 가입 신청
- `POST /wiz/api/layout.sidebar/change_password` - 본인 비밀번호 변경
- `GET /auth/check` - 세션 확인
- `GET /auth/logout` - 로그아웃

### 명함
- `GET /wiz/api/page.cards/list` - 명함 검색
- `POST /wiz/api/page.cards/analyze` - 명함 이미지 서버 OCR 분석
- `POST /wiz/api/page.cards/preview_import` - CSV/TXT/XLSX 명함 가져오기 미리보기
- `POST /wiz/api/page.cards/import_cards` - 컬럼 매핑 기반 명함 일괄 가져오기
- `POST /wiz/api/page.cards/export_cards` - 현재 검색 조건 기준 CSV/XLSX 내보내기
- `GET /wiz/api/page.cards/get` - 명함 단건 조회
- `POST /wiz/api/page.cards/save` - 분석/검토된 명함 등록 또는 수정
- `POST /wiz/api/page.cards/remove` - 명함 삭제 처리

### 사용자 관리
- `GET /wiz/api/page.users/list` - 사용자 목록
- `POST /wiz/api/page.users/approve` - 가입 승인
- `POST /wiz/api/page.users/activate` - 계정 활성화
- `POST /wiz/api/page.users/block` - 계정 차단
- `POST /wiz/api/page.users/update_role` - 권한 변경. 초기 관리자와 마지막 활성 관리자의 사용자 권한 강등은 차단됩니다.
