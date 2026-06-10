# 명함장

[English](README.en.md) · [Demo](https://bus.sub.nanoha.kr/)

[WIZ Framework](https://github.com/season-framework/wiz) 기반 개인용 명함 관리 PWA와 Android 전화 수신 명함 표시 앱입니다. 사진 촬영/업로드 OCR, CSV/TXT/XLSX 가져오기, CSV/XLSX 내보내기, 관리자 승인 기반 사용자 관리, AI OCR Provider 설정, Android 명함 동기화와 수신 전화 표시까지 현재 개발 범위에 포함되어 있습니다.

이 프로젝트는 AI 기반 개발 지원을 활용해 개발되었습니다.

## 현재 개발 상태

- 일반 사용자는 로그인 후 `/cards`에서 명함 검색, 정렬, 페이지 이동, 상세 확인, 수정, 삭제, 사진 분석 등록, 파일 가져오기/내보내기를 사용합니다.
- 관리자는 상단 앱바에서 `/users` 사용자 관리와 `/ai-settings` AI 설정 화면으로 이동할 수 있습니다.
- PWA 시작 URL은 `/cards`이며, 서비스워커와 manifest 라우트가 구성되어 있습니다.
- 명함 OCR은 서버 Tesseract 분석을 기본으로 하고, 관리자가 활성화한 OpenAI/Google/Ollama Vision Provider를 AI fallback으로 사용할 수 있습니다.
- 파일 가져오기는 CSV/TXT/XLSX를 지원하고, 자동 컬럼 매핑, 중복 처리, 매핑 안 된 컬럼 메모 병합 옵션을 제공합니다.
- 내보내기는 현재 검색 조건을 기준으로 CSV 또는 XLSX 포맷을 선택해 내려받습니다.
- Android 앱은 `android/`에서 Gradle로 빌드하며, WIZ 모바일 API로 로그인, 명함 증분 동기화, 이미지 캐시, 수신 전화 명함 오버레이/알림 표시를 수행합니다.

## 스크린샷

ReviewOps 첨부 스크린샷을 README용 자산으로 반영했습니다. 목록 화면은 실제 등록 명함의 이름, 회사, 연락처가 포함되어 있어 더미 데이터로 익명화한 파생 이미지만 저장했습니다.

| 로그인 | 명함 목록 |
|--------|-----------|
| <img src="docs/screenshots/login.png" width="260" alt="로그인 화면"> | <img src="docs/screenshots/card-list-sanitized.png" width="260" alt="익명화된 명함 목록 화면"> |

| 사진 분석 등록 | 파일 가져오기 |
|----------------|---------------|
| <img src="docs/screenshots/card-photo-register.png" width="260" alt="사진 분석 등록 화면"> | <img src="docs/screenshots/card-import.png" width="260" alt="파일 가져오기 화면"> |

## 초기 접속 흐름

1. `/access`에서 로그인하거나 가입 신청을 합니다.
2. 첫 번째 가입 계정은 초기 부트스트랩을 위해 `admin / active` 상태로 생성됩니다.
3. 이후 가입 계정은 `user / pending` 상태로 생성되며, 관리자가 `/users`에서 승인해야 로그인할 수 있습니다.
4. 로그인 후 기본 진입점과 PWA 시작 URL은 `/cards`입니다.

## 주요 화면

- `/access`: 로그인과 가입 신청
- `/cards`: 명함 목록, 검색, 정렬, 페이지네이션, 상세 확인, 사진 OCR 등록, 파일 가져오기/내보내기
- `/my-card`: 내 명함 작성, 디자인 저장, 이미지 공유, 공개 링크, Android APK 다운로드
- `/users`: 관리자 전용 사용자 승인, 활성화/차단, 권한 변경
- `/ai-settings`: 관리자 전용 AI OCR Provider, 모델, API Key 설정

## Android 앱

네이티브 앱 코드는 `android/` 아래에 있으며 Galaxy / One UI 8.5 이상, JDK 17, Compile/Target SDK 36 기준으로 구성되어 있습니다. 자세한 SDK 설치와 실기 실행 절차는 [android/README.md](android/README.md)를 확인합니다.

- 서버 주소는 Android `BuildConfig.WEB_BASE_URL`의 `https://bus.sub.nanoha.kr/`를 사용합니다.
- 앱은 `/api/mobile/...` 라우트에 로그인해 명함, 전화번호 alias, 오버레이 이미지를 증분 동기화합니다.
- access/refresh token은 Android Keystore 기반 저장소에 보관하고, 명함과 이미지 캐시는 앱 전용 SQLite/파일 저장소에 저장합니다.
- 실제 수신 전화는 `CallScreeningService`와 `PHONE_STATE` fallback으로 감지하며, 번호 매칭 성공 시 명함 이미지 또는 heads-up 알림과 최근 통화/SMS 이력을 표시합니다.
- `/my-card` 모바일 화면에서 APK 다운로드 버튼을 제공하고, `/download/android-app.apk` 라우트가 `android/app/build/outputs/apk/debug/app-debug.apk`를 내려줍니다.

```bash
cd android
./scripts/setup-android-sdk.sh
./gradlew :app:assembleDebug
```

## 주요 구조

```text
src/
├── app/
│   ├── page.access/        # 로그인 / 가입 신청
│   ├── page.cards/         # 명함 OCR / 목록 / 상세 / 가져오기 / 내보내기
│   ├── page.my_card/       # 내 명함 작성 / 공유 / Android APK 다운로드
│   ├── page.users/         # 관리자 전용 사용자 승인/관리
│   ├── page.ai_settings/   # 관리자 전용 AI OCR Provider 설정
│   └── layout.sidebar/     # 인증 후 상단 앱바 공통 레이아웃
├── route/
│   ├── manifest/           # /manifest.json PWA manifest
│   ├── mobile-api/         # Android 앱 인증 / sync / 이미지 API
│   └── android-apk-download/ # /download/android-app.apk
├── controller/
│   ├── base.py             # 세션 초기화 및 요청 파싱
│   ├── user.py             # 로그인/활성 상태/세션 토큰 검증
│   └── admin.py            # 관리자 권한 검증
└── model/
    ├── db/
    │   ├── user.py
    │   ├── user_session.py
    │   ├── access_log.py
    │   ├── ai_setting.py
    │   └── business_card.py
    └── struct/
        ├── user.py
        ├── user_session.py
        ├── access_log.py
        ├── ai_setting.py
        └── business_card.py
android/
└── app/                    # Android 전화 수신 명함 표시 앱
```

## 로컬/운영 설정

`config/`는 프로젝트별 로컬 설정 디렉터리이며 git에 올리지 않습니다. 저장소에는 안전한 예시 파일인 `config-sample/database.py`만 추적합니다.

운영 DB 설정은 배포 환경에서 `config/database.py` 또는 런타임 환경 변수로 주입합니다. 실제 비밀번호, API Key, 세션 secret, DB 백업 파일은 저장소에 커밋하지 않습니다.

| 환경 변수 | 설명 |
|----------|------|
| `DB_TYPE` | `mariadb`, `mysql`, `sqlite` 등 실행 DB 종류 |
| `DB_HOST` | DB 호스트 |
| `DB_PORT` | DB 포트 |
| `DB_NAME` | DB 이름 |
| `DB_USER` | DB 사용자 |
| `DB_PASSWORD` | 운영 secret으로만 주입하는 DB 비밀번호 |

## 민감 정보와 git 관리

- `.gitignore`는 `config/`, `.env*`, `data/`, 로컬 DB 파일, key/pem/secret 파일, 빌드/테스트 산출물을 제외합니다.
- `config-sample/`에는 실제 접속 정보가 아닌 샘플만 둡니다.
- AI Provider API Key는 조회 응답에서 원문을 반환하지 않고 `has_api_key` 상태만 반환합니다. 다만 DB에는 호출 가능한 형태로 저장되므로 운영 DB와 백업은 secret으로 취급해야 합니다.
- 이미 추적된 로컬 secret 파일이 생기면 값을 삭제한 뒤 `git rm --cached <path>`로 index에서 제거하고, 필요하면 secret rotation을 수행합니다.

## OCR 분석 전략

- `POST /wiz/api/page.cards/analyze`: 앞면/뒷면 이미지를 받아 EXIF 방향 보정, 이미지 전처리, Tesseract `kor+eng` OCR을 수행합니다.
- 앞면/뒷면을 함께 분석하면 앞면 필드를 우선하고 빈 값만 뒷면 결과로 보완합니다.
- 기본 OCR은 명함 사진에 강한 `psm 11`을 사용하고, 필수 필드가 부족할 때 `psm 6` 보완 패스를 실행합니다.
- 원본 OCR 결과가 부족하면 90도, 270도, 180도 회전 후보를 추가 분석해 점수가 높은 방향을 선택합니다.
- 서버 OCR이 부족하고 AI 설정이 활성화되어 있으면 OpenAI, Google Gemini, Ollama Vision 계열 Provider를 fallback으로 사용할 수 있습니다.
- 서버 OCR 활성화에는 Python 의존성 외에 OS 패키지 `tesseract-ocr`, `tesseract-ocr-kor`, `tesseract-ocr-eng`가 필요합니다.

## 주요 API

### 인증

- `POST /wiz/api/page.access/login` - 로그인
- `POST /wiz/api/page.access/signup` - 가입 신청
- `POST /wiz/api/layout.sidebar/change_password` - 본인 비밀번호 변경
- `GET /auth/check` - 세션 확인
- `GET /auth/logout` - 로그아웃

### 명함

- `GET /wiz/api/page.cards/list` - 명함 검색/정렬/페이지네이션
- `POST /wiz/api/page.cards/analyze` - 명함 이미지 서버 OCR/AI fallback 분석
- `POST /wiz/api/page.cards/preview_import` - CSV/TXT/XLSX 가져오기 미리보기
- `POST /wiz/api/page.cards/import_cards` - 컬럼 매핑 기반 명함 일괄 가져오기
- `POST /wiz/api/page.cards/export_cards` - 현재 검색 조건 기준 CSV/XLSX 내보내기
- `GET /wiz/api/page.cards/get` - 명함 단건 조회
- `POST /wiz/api/page.cards/save` - 명함 등록 또는 수정
- `POST /wiz/api/page.cards/remove` - 명함 삭제 처리

### Android 모바일

- `GET /api/mobile/health` - 모바일 API 상태 확인
- `POST /api/mobile/auth/login` - Android 앱 로그인 및 토큰 발급
- `POST /api/mobile/auth/refresh` - access token 갱신
- `POST /api/mobile/auth/logout` - 모바일 세션 종료
- `GET /api/mobile/cards/sync?since={iso8601}` - 명함/전화번호 alias 증분 동기화
- `POST /api/mobile/cards/overlay-images` - 오버레이 이미지 배치 다운로드
- `GET /api/mobile/cards/{id}/overlay-image?kind=front|back|generated` - 단일 오버레이 이미지 다운로드
- `POST /api/mobile/devices/{id}/overlay-settings` - Android 표시 설정 저장
- `GET /download/android-app.apk` - Android debug APK 다운로드

### 관리자

- `GET /wiz/api/page.users/list` - 사용자 목록
- `POST /wiz/api/page.users/approve` - 가입 승인
- `POST /wiz/api/page.users/activate` - 계정 활성화
- `POST /wiz/api/page.users/block` - 계정 차단
- `POST /wiz/api/page.users/update_role` - 권한 변경
- `GET /wiz/api/page.ai_settings/get_setting` - AI 설정 조회
- `POST /wiz/api/page.ai_settings/models` - Provider 모델 목록 조회
- `POST /wiz/api/page.ai_settings/save` - AI 설정 저장

## 검증

Playwright 테스트는 `season-wiz-project=main`, `season-wiz-devmode=true` 쿠키를 주입합니다.

```bash
npm run playwright:install-deps
npm run playwright:install
npm run test:e2e
```

기본 테스트 대상은 `https://bus.sub.nanoha.kr`이며, 다른 환경은 `PLAYWRIGHT_BASE_URL`로 지정합니다.

서버 로직 smoke 테스트는 프로젝트 루트에서 실행합니다.

```bash
python tests/import_export_smoke.py
python tests/ocr_business_card_smoke.py
```

OCR 이미지 smoke 테스트는 `data/` 아래 실제 샘플 이미지와 Tesseract 런타임이 필요합니다.

## 라이선스

이 프로젝트는 [MIT License](LICENSE)를 따릅니다.
