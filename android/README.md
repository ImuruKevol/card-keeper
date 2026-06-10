# Android 개발 환경

명함장 전화 수신 오버레이 기능을 위한 Android 네이티브 앱 프로젝트입니다. 기존 WIZ/PWA 프로젝트와 분리해 `android/` 아래에서 Gradle로 빌드합니다.

## 기준

- Galaxy / One UI 8.5 이상 전용
- Android Gradle Plugin 9.2.0
- Gradle Wrapper 9.4.1
- JDK 17
- Compile/Target SDK 36
- Min SDK 36
- Android SDK Build Tools 36.0.0

AGP 9.2.0은 Android Developers 공식 릴리스 노트 기준으로 Gradle 9.4.1, JDK 17, Build Tools 36.0.0을 사용합니다.

## 초기 설정

```bash
cd android
./scripts/setup-android-sdk.sh
```

스크립트는 Android command-line tools를 `android/.android-sdk/`에 설치하고 `local.properties`에 `sdk.dir`을 기록합니다. 두 경로는 git에 포함하지 않습니다.

## 빌드

```bash
cd android
./gradlew :app:assembleDebug
```

APK 산출물:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

## 구현된 MVP 범위

- `MainActivity`: 모바일 로그인, 명함 동기화, 역할/오버레이 권한 요청, 위치 프리셋, 테스트 오버레이 실행
- `MobileApiClient`: WIZ 모바일 API 로그인, refresh, sync, overlay image 다운로드
- `MobileTokenStore`: Android Keystore 기반 토큰 암호화 저장
- `BusinessCardDatabase`: SQLite 로컬 명함/전화번호 인덱스/이미지 캐시 저장
- `BusinessCardSyncManager`: 서버 증분 sync, 오버레이 이미지 파일 캐시, access token 만료 시 refresh 재시도
- `BusinessCardCallScreeningService`: 수신 번호 정규화 후 오버레이 서비스 호출
- `IncomingCallReceiver`: `PHONE_STATE` 기반 실제 수신 전화 감지 fallback
- `BusinessCardDisplayController`: 로컬 DB 번호 매칭, 표시 모드에 따른 오버레이/heads-up 알림 분기, 최근 통화/SMS 이력 표시
- `BusinessCardOverlayService`: 테스트 오버레이 표시, drag 위치 저장, 오버레이 권한이 없을 때 heads-up 알림 fallback
- `BusinessCardNotification`: 알림 전용 표시와 명함 이미지 big picture/최근 기록 요약
- `CardImageRenderer`: 이미지가 없는 명함을 위한 1200x680 가상 명함 Bitmap 렌더러
- `PhoneNumberNormalizer`: 국내형/82 prefix 전화번호 alias 정규화

## 서버 API

Android 앱은 WIZ 서버의 `/api/mobile/...` 라우트를 사용합니다.

```text
GET  /api/mobile/health
POST /api/mobile/auth/login
POST /api/mobile/auth/refresh
POST /api/mobile/auth/logout
GET  /api/mobile/cards/sync?since={iso8601}
POST /api/mobile/cards/overlay-images
GET  /api/mobile/cards/{id}/overlay-image?kind=front|back|generated
POST /api/mobile/devices/{id}/overlay-settings
```

명함 sync는 삭제 tombstone, 전화번호 alias, `front/back/generated` 이미지 hash, 오버레이 이미지 종류를 내려줍니다. 앱은 hash가 바뀐 이미지만 배치 다운로드해 앱 전용 저장소에 캐시합니다.

## 실기 실행

1. Galaxy One UI 8.5 이상 기기에서 USB debugging을 켭니다.
2. `adb devices`로 연결을 확인합니다.
3. debug APK를 설치합니다.

```bash
cd android
./.android-sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

4. 앱에서 로그인 후 명함을 동기화합니다.
5. 발신자 표시 역할과 오버레이 권한을 허용합니다.
6. 테스트 오버레이로 위치를 확인하고 실제 수신 전화에서 매칭 동작을 검증합니다.

## 남은 구현 과제

- WorkManager 기반 주기적/재부팅 후 자동 sync
- Room/Retrofit 등 Jetpack 스택 전환 여부 결정
- 이미지 캐시 용량 제한과 오래된 파일 정리
- 실제 One UI 8.5 이상 Galaxy 기기 QA 매트릭스 정리
- 관리자용 모바일 기기 폐기 화면 추가
