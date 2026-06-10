# Android 실제 수신 전화 감지 fallback 보강

- **ID**: 010
- **날짜**: 2026-06-10
- **유형**: 버그 수정

## 작업 요약

갤럭시 폴드7 One UI 8.5에서 실제 전화 수신 시 오버레이/알림이 표시되지 않는 문제를 완화하기 위해 `CallScreeningService` 표시 흐름을 공통 dispatcher로 정리하고, 기본 앱 콜백이 제한될 때 `PHONE_STATE` 브로드캐스트로 수신 번호를 감지하는 fallback을 추가했다. 실제 수신 경로에서 오버레이 렌더링 실패가 비동기로 누락되지 않도록 메인 스레드 즉시 렌더링 및 알림 fallback도 보강했다.

## 원문 요청사항

```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

여전히 전화가 왔을 때 갤럭시 폴드7에서 알림과 오버레이가 뜨지 않고 있어.

## 리뷰 요약

- 리뷰 ID: ybauiwwadospkltesipbfwwcnfcdnqyu
- 제목: 안드로이드 앱 개발
- 요청 링크: https://bus.sub.nanoha.kr/
- Codex 요청자: 권태욱
- 프로젝트 루트: /opt/app
- Codex 세션 ID: 019eab86-cd6d-7872-994e-a574aebc5f4c
- 스크린샷 컨텍스트: 없음
- 첨부파일 컨텍스트: 0개
```

## 변경 파일 목록

- `android/app/src/main/AndroidManifest.xml`
  - 전화 상태/전화번호 권한과 `PHONE_STATE` 수신 receiver를 추가했다.
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/MainActivity.kt`
  - 전화 상태 감지 권한 상태와 권한 요청 버튼을 설정 화면에 추가했다.
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/telecom/IncomingCallerDisplay.kt`
  - 수신 번호 기반 명함 조회, 명함 없음 placeholder, 최근 기록 조회, 오버레이/알림 선택 표시를 공통화했다.
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/telecom/IncomingCallReceiver.kt`
  - `PHONE_STATE` ringing 이벤트에서 수신 번호를 받아 공통 표시 흐름을 호출하도록 추가했다.
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/telecom/BusinessCardCallScreeningService.kt`
  - 기본 앱 콜백도 공통 표시 흐름을 사용하도록 정리하고 응답 처리를 유지했다.
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/overlay/BusinessCardDisplayController.kt`
  - 실제 수신 경로에서 오버레이를 메인 스레드에서 즉시 렌더링하고, 실패 시 알림으로 fallback하도록 보강했다.
- `devlog.md`
  - 2026-06-10 작업 요약 행을 추가했다.
- `devlog/2026-06-10/010-android-phone-state-fallback-display.md`
  - 작업 상세 기록을 추가했다.

## 검증 결과

- `./gradlew :app:assembleDebug` 성공
  - debug APK 패키징 완료
  - `statusBarColor`, `navigationBarColor`, `TelephonyManager.EXTRA_INCOMING_NUMBER` deprecation 경고가 있으나 빌드는 정상 완료
- APK 다운로드 라우트 확인
  - `curl` 응답: `200 application/vnd.android.package-archive 2637049`
  - 다운로드 파일과 새 debug APK SHA256 일치
  - SHA256: `887c1f09326200892f13df5215fdd792e99614760540b6dc02afc0b98e62cd2c`

## 남은 리스크

- `PHONE_STATE`의 수신 번호 extra는 deprecated API라서 일부 Android/One UI 정책에서는 권한을 허용해도 번호가 비어 있을 수 있다.
- 새 APK 설치 후 앱에서 전화 상태/전화번호 권한을 허용해야 fallback이 동작한다.
- 실제 갤럭시 폴드7 One UI 8.5 기기에서 수신 전화 테스트는 이 환경에서 직접 수행할 수 없다.
