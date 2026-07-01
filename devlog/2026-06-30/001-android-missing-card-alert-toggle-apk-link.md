# Android 미등록 번호 알림 토글 및 APK 링크 갱신

## 사용자 원 요청

작업 시작.
수정 후에는 apk로 새롭게 빌드 및 내 명함첩 화면에 있는 링크도 수정해야해

리뷰 요청: 전화가 왔을 때 저장된 명함 목록 안에 없을 때 알림을 띄우지 않도록 하는 옵션을 추가하고, 활성화/비활성화할 수 있도록 한다.

## 작업 요약

- Android 앱 기능 설정 화면에 "저장되지 않은 번호 알림" 토글을 추가했다.
- 토글이 비활성화된 경우 로컬 명함 DB에서 번호가 매칭되지 않는 수신 전화는 미등록 번호 알림을 만들지 않도록 했다.
- Android 앱 버전을 `0.1.1`로 올리고 debug APK를 새로 빌드했다.
- 내 명함 화면의 APK 다운로드 링크와 다운로드 파일명을 `business-card-caller-0.1.1.apk`로 갱신했다.

## 변경 파일

- `android/app/build.gradle.kts`
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/MainActivity.kt`
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/overlay/OverlaySettingsStore.kt`
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/telecom/IncomingCallerDisplay.kt`
- `src/app/page.my_card/view.pug`
- `src/route/android-apk-download/controller.py`
- `devlog.md`
- `devlog/2026-06-30/001-android-missing-card-alert-toggle-apk-link.md`

## 검증

- `./gradlew :app:assembleDebug` 성공
- `android/app/build/outputs/apk/debug/app-debug.apk` 생성 확인
- `aapt dump badging`으로 `versionCode='2'`, `versionName='0.1.1'` 확인
- `wiz_project_build(clean=false)` 성공
- `rg`로 빌드 산출물의 `/download/android-app.apk?v=0.1.1` 및 `business-card-caller-0.1.1.apk` 반영 확인
- `curl -I`에 `season-wiz-project=main`, `season-wiz-devmode=true` 쿠키를 넣어 `/download/android-app.apk?v=0.1.1` 응답이 `200 OK`, `filename=business-card-caller-0.1.1.apk`임을 확인

## 남은 리스크

- 실제 Android 기기에서 수신 전화 권한 조합과 토글 동작은 로컬 환경에서 실기 검증하지 못했다.
