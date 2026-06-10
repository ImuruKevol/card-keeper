# Android 최근 통화 기록 통화 시간 표시 보강

- **ID**: 008
- **날짜**: 2026-06-10
- **유형**: 기능 개선

## 작업 요약

오버레이/알림에 표시되는 최근 통화 기록에서 통화 시간을 초 단위 숫자로만 표시하던 부분을 분/초 형식으로 개선했다. 1분 이상 통화는 `N분 M초`, 1시간 이상 통화는 `N시간 M분` 형식으로 표시된다.

## 원문 요청사항

```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

오버레이/알림에 최근 통화 및 문자 기록 5건을 표시할 때 통화에 대해서는 몇 초/분동안 통화를 했는지도 표시를 해줘.

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

- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/data/ContactHistoryReader.kt`
  - `formatDuration` helper를 추가했다.
  - 통화 기록의 `duration` 값을 `초`, `분 초`, `시간 분` 형식으로 변환해 표시하도록 변경했다.
- `devlog.md`
  - 2026-06-10 작업 요약 행을 추가했다.
- `devlog/2026-06-10/008-android-call-history-duration-format.md`
  - 작업 상세 기록을 추가했다.

## 검증 결과

- `./gradlew :app:assembleDebug` 성공
  - Kotlin 컴파일 및 debug APK 패키징 완료
- APK 다운로드 라우트 확인
  - `curl` 응답: `200 application/vnd.android.package-archive 2719797`
  - 다운로드 파일과 새 debug APK SHA256 일치
  - SHA256: `b09d46b77f014c41bef573b49e73392e1a63721c0f553f5eb701dfa8a258efd3`

## 남은 리스크

- 실제 통화 기록의 duration 값은 Android CallLog provider가 제공하는 값에 의존한다.
- 부재중/거절/차단 통화처럼 duration이 0인 기록은 통화 시간 텍스트가 표시되지 않는다.
