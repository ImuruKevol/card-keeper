# Android 앱 빌드 및 개발 환경 구성

- **ID**: 019
- **날짜**: 2026-06-09
- **유형**: 설정 변경

## 작업 요약

Android 앱 개발을 시작할 수 있도록 `android/` Gradle 프로젝트를 신규 구성했다.
AGP 9.2.0, Gradle Wrapper 9.4.1, JDK 17, Android SDK 36 기준으로 debug APK 빌드가 가능한 최소 네이티브 앱 골격을 추가했다.

## 원문 요청사항

```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

일단 안드로이드 앱 빌드 및 개발을 위한 환경을 구성해줘.

## 리뷰 요약

- 리뷰 ID: fsumlylodsndjbhmyxpxsxyukprfwbzd
- 제목: 안드로이드 앱 개발 설계
- 요청 링크: https://bus.sub.nanoha.kr/
- Codex 요청자: 권태욱
- 프로젝트 루트: /opt/app
- Codex 세션 ID: 019eaa55-14cb-7ad0-9f3b-b5c8915b0f76
- 스크린샷 컨텍스트: 없음
- 에이전트 작업 지시서 컨텍스트: 없음
- HTML 문서 생성 규칙 컨텍스트: 없음
- HTML 문서 설정 컨텍스트: 없음
- HTML 프로젝트 인스트럭션 파일: 없음
- 첨부파일 컨텍스트: 0개

## 세션 처리

저장된 Codex 세션을 resume해 이전 대화 맥락을 우선 사용하세요. 이전 Codex 히스토리는 이 요청에 포함되지 않습니다.
```

## 변경 파일 목록

- `.gitignore`: Android SDK, Gradle 캐시, local.properties, 빌드 산출물 제외 규칙 추가
- `android/README.md`: Android 개발/빌드 방법 문서 추가
- `android/settings.gradle.kts`: Android Gradle 프로젝트 설정 추가
- `android/build.gradle.kts`: AGP 9.2.0 루트 플러그인 설정 추가
- `android/gradle.properties`: Gradle/Android 빌드 속성 추가
- `android/gradlew`, `android/gradlew.bat`, `android/gradle/wrapper/*`: Gradle Wrapper 9.4.1 추가
- `android/app/build.gradle.kts`: 앱 모듈 설정 추가
- `android/app/src/main/AndroidManifest.xml`: Activity, CallScreeningService, OverlayService 선언 추가
- `android/app/src/main/res/values/*`: 앱 이름/테마 리소스 추가
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/*`: MainActivity, 전화 선별 서비스, 오버레이 서비스, 번호 정규화 유틸 추가
- `android/scripts/setup-android-sdk.sh`: 로컬 Android SDK 설치 스크립트 추가
- `devlog.md`: 이번 작업 요약 행 추가
- `devlog/2026-06-09/019-android-build-environment.md`: 이번 작업 상세 기록 추가

## 로컬 환경 구성

- `openjdk-17-jdk-headless`, `wget`, `unzip`, `ca-certificates` 패키지 설치
- Android command-line tools를 `android/.android-sdk/`에 설치
- `platform-tools`, `platforms;android-36`, `build-tools;36.0.0` 설치
- `android/local.properties`에 로컬 SDK 경로 기록
- `android/.android-sdk/`, `android/.gradle/`, `android/local.properties`, `android/app/build/`는 git ignore 처리

## 검증 결과

- `java -version` 확인: OpenJDK 17.0.19
- `./gradlew --version` 확인: Gradle 9.4.1, JVM 17.0.19
- `./scripts/setup-android-sdk.sh` 성공
- `./gradlew :app:assembleDebug` 성공
- APK 산출물 확인: `android/app/build/outputs/apk/debug/app-debug.apk` (2.4M)
- `git status --short --ignored android`로 로컬 SDK/빌드 산출물이 ignore 처리되는 것을 확인

## 남은 리스크

- 현재 Android 앱은 빌드 가능한 개발 베이스이며, 실제 명함 sync API, Room DB, 명함 이미지 캐시, 가상 명함 렌더러, 위치 조정 UI는 아직 구현 전이다.
- `CallScreeningService`와 `SYSTEM_ALERT_WINDOW` 오버레이는 실제 One UI 8.5 이상 Galaxy 기기에서 역할/권한 부여 후 실기 검증이 필요하다.
