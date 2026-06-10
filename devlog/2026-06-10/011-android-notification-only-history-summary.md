# Android 알림 전용 명함 표시 및 기록 요약 개선

- **ID**: 011
- **날짜**: 2026-06-10
- **유형**: 기능 개선

## 작업 요약

오버레이 방식이 기본 전화 앱/에이닷 오버레이와 충돌할 수 있어 앱 사용자 흐름에서 제거하고, 실제 수신 표시와 테스트 표시를 알림 방식으로 고정했다. 알림은 명함 이미지를 접힌 상태에서도 최대한 보이도록 BigPicture collapsed 표시를 켰고, 최근 한 달 통화/문자 기록 건수와 오늘/상대시간이 포함된 최근 기록 문구를 표시하도록 개선했다.

## 원문 요청사항

```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

오버레이 방식은 버려야 할 것 같아.
오버레이 방식은 기본 전화 앱이나 에이닷 모두 오버레이 형식으로 이미 떠있어서 오버레이가 먹히거나 작동하지 않는 것 같아.
대신 알림 방식은 제대로 잘 작동하고 있어. 근데 기본적으로 이미지는 해당 알림을 아래로 내려서 상세 보기를 해야 명함 이미지가 뜨는데, 기본적으로 펼치기로 할 수 있으면 좋겠어.
그리고 알림에서는 그냥 가장 최근 기록(통화/메세지) 하나만 표시가 되는데, 최근 한달간 몇 건 기록이 있는지도 표시를 해주면 좋겠어. 그리고 당일일 경우엔 그냥 시간만 띡 표시가 되는데, 오늘이면 오늘 텍스트를 표시하도록 해서 가독성을 높여줘.  그리고 몇 분/시간/일 전인지도 추가해주고.

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
  - `SYSTEM_ALERT_WINDOW` 권한과 오버레이 서비스 등록을 제거했다.
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/MainActivity.kt`
  - 표시 방식을 알림 전용으로 단순화하고 오버레이 권한/위치 UI를 제거했다.
  - 표시 테스트가 오버레이 서비스 대신 알림 생성기를 직접 호출하도록 변경했다.
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/telecom/IncomingCallerDisplay.kt`
  - 실제 수신 전화 표시를 항상 알림 방식으로 처리하도록 변경했다.
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/data/ContactHistoryReader.kt`
  - 최근 5건과 최근 한 달 기록 수를 함께 담는 `ContactHistorySummary`를 추가했다.
  - 당일 기록은 `오늘 HH:mm`, 모든 기록은 `n분/n시간/n일 전` 형태를 함께 표시하도록 포맷을 개선했다.
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/overlay/BusinessCardNotification.kt`
  - 알림 collapsed 상태에서 BigPicture 이미지를 최대한 표시하도록 `showBigPictureWhenCollapsed(true)`를 적용했다.
  - 최근 한 달 건수와 최근 기록 요약을 알림 본문/상세에 표시하도록 변경했다.
- `devlog.md`
  - 2026-06-10 작업 요약 행을 추가했다.
- `devlog/2026-06-10/011-android-notification-only-history-summary.md`
  - 작업 상세 기록을 추가했다.

## 검증 결과

- `./gradlew :app:assembleDebug` 성공
  - debug APK 패키징 완료
  - `statusBarColor`, `navigationBarColor`, notification priority deprecation 경고가 있으나 빌드는 정상 완료
- APK 다운로드 라우트 확인
  - `curl` 응답: `200 application/vnd.android.package-archive 2638689`
  - 다운로드 파일과 새 debug APK SHA256 일치
  - SHA256: `a639d783f2478a1e1b31b77be4083c48b4c4606d4340322518c61c756968ef69`

## 남은 리스크

- Android 알림의 기본 펼침 여부는 OS/제조사 알림 정책이 최종 결정하므로 앱에서 완전 강제할 수 없다. Android 12 이상에서는 접힌 상태 이미지 표시를 요청하도록 보강했다.
- 최근 한 달 건수는 통화 기록/문자 권한과 기기 ContentProvider 조회 결과에 의존한다.
- 실제 갤럭시 폴드7 One UI 8.5 알림 렌더링은 이 환경에서 직접 확인할 수 없다.
