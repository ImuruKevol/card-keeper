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

## 현재 스캐폴드

- `MainActivity`: 역할 요청, 오버레이 권한 설정, 웹 명함장 열기 버튼 제공
- `BusinessCardCallScreeningService`: 전화 수신 번호를 받아 정규화 후 오버레이 서비스 호출
- `BusinessCardOverlayService`: `TYPE_APPLICATION_OVERLAY` 기반 임시 오버레이 표시
- `PhoneNumberNormalizer`: 한국 전화번호 alias 정규화 유틸

다음 단계는 WIZ 모바일 sync API, Room 로컬 DB, 명함 이미지 캐시, `/my-card` 스타일 가상 명함 이미지 렌더러, 위치 조정 UI를 붙이는 것입니다.

## 실기 실행

1. Galaxy One UI 8.5 이상 기기에서 USB debugging을 켭니다.
2. `adb devices`로 연결을 확인합니다.
3. debug APK를 설치합니다.

```bash
cd android
./.android-sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

4. 앱에서 발신자 표시 역할과 오버레이 권한을 허용합니다.
