# Android 명함 표시 방식 오버레이/알림 선택 추가

- **ID**: 005
- **날짜**: 2026-06-10
- **유형**: 기능 추가

## 작업 요약

Android 앱에서 수신 명함 표시 방식을 사용자가 `오버레이` 또는 `알림` 중 선택할 수 있도록 구현했다. 기존 `BusinessCardNotification`을 재사용해 알림 모드에서는 화면 위 오버레이 대신 알림 영역에 명함 이미지와 정보를 표시하고, 오버레이 권한이 없을 때도 알림으로 fallback되도록 기존 흐름을 유지했다.

## 원문 요청사항

```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

그럼 오버레이 방식 대신 푸시 알림 영역에 명함 이미지와 정보를 표시한다던가 하는 기능 추가는 가능할까?
어떤 방식으로 사용할지는 사용자가 선택하도록 하고 싶어.

## 리뷰 요약

- 리뷰 ID: ybauiwwadospkltesipbfwwcnfcdnqyu
- 제목: 안드로이드 앱 개발
- 요청 링크: https://bus.sub.nanoha.kr/
- Codex 요청자: 권태욱
- 프로젝트 루트: /opt/app
- Codex 세션 ID: 019eab86-cd6d-7872-994e-a574aebc5f4c
- 스크린샷 컨텍스트: 없음
- 에이전트 작업 지시서 컨텍스트: 없음
- HTML 문서 생성 규칙 컨텍스트: 없음
- HTML 문서 설정 컨텍스트: 없음
- HTML 프로젝트 인스트럭션 파일: 없음
- 첨부파일 컨텍스트: 0개
```

## 변경 파일 목록

- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/overlay/OverlaySettingsStore.kt`
  - `CardDisplayMode` enum을 추가했다.
  - `OverlaySettings`에 `displayMode`를 추가했다.
  - 표시 방식 저장/로드를 위한 `saveDisplayMode`와 `display_mode` preference key를 추가했다.
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/overlay/BusinessCardOverlayService.kt`
  - 표시 방식이 `NOTIFICATION`이면 오버레이 대신 `BusinessCardNotification.show`를 호출하도록 분기했다.
  - 오버레이 권한이 없을 때 알림 fallback되는 기존 동작은 유지했다.
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/MainActivity.kt`
  - 설정 화면에 `표시 방식` 카드와 `오버레이`/`알림` 선택 버튼을 추가했다.
  - 현재 표시 방식을 요약에 표시하도록 했다.
  - 알림 방식 설명과 선택 상태 버튼 스타일을 추가했다.
  - 테스트 버튼이 현재 선택 방식에 맞춰 오버레이 또는 알림을 표시하도록 변경했다.
- `devlog.md`
  - 2026-06-10 작업 요약 행을 추가했다.
- `devlog/2026-06-10/005-android-card-display-mode-notification.md`
  - 작업 상세 기록을 추가했다.

## 검증 결과

- `./gradlew :app:assembleDebug` 성공
  - Kotlin 컴파일 및 debug APK 패키징 완료
  - 기존과 동일하게 `statusBarColor`, `navigationBarColor` deprecated 경고 2건이 있으나 빌드는 정상 완료
- APK 다운로드 라우트 확인
  - `curl` 응답: `200 application/vnd.android.package-archive 2596733`
  - 다운로드 파일과 새 debug APK SHA256 일치
  - SHA256: `a27d949890f24e12f976841a0a3aa87a55719be8cbb54a0a9ee1a1d0daafc8c6`

## 남은 리스크

- 알림 방식도 수신 전화 번호를 자동으로 받으려면 여전히 Android `발신자 표시 기본 앱` 역할이 필요하다.
- 알림 모드는 Android 알림 권한이 거부되어 있으면 표시되지 않으므로 실제 단말에서 권한 허용 흐름 확인이 필요하다.
- 실제 수신 전화 상황에서 BigPicture 알림 이미지 렌더링과 잠금화면 노출 형태는 기기별 확인이 필요하다.
