# Android 실제 수신 전화 표시 연결 보강

- **ID**: 006
- **날짜**: 2026-06-10
- **유형**: 버그 수정

## 작업 요약

표시 테스트는 동작하지만 실제 수신 전화에서 오버레이/알림이 표시되지 않는 문제를 보강했다. `CallScreeningService`가 수신 콜백에서 5초 안에 `respondToCall`을 호출하도록 수정하고, 수신 번호를 서비스 내부에서 직접 명함 DB와 매칭한 뒤 선택된 표시 방식으로 연결하도록 변경했다. 연락처에 저장된 번호도 screening 대상이 되도록 `READ_CONTACTS` 권한을 추가하고 설정 화면에 권한 상태/허용 버튼을 추가했다.

## 원문 요청사항

```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

기본 앱으로 설정 버튼은 현재 설정되어 있으면 기본 앱을 선택할 수 있는 UI가 안뜨는데, 설정이 되어있어도 그 UI가 뜨도록 해줘.
그리고 오버레이, 알림 둘다 동작하지 않고 있어.
나는 갤럭시 폴드7 One UI 8.5를 사용하고 있는데, 등록된 명함의 사람에게 부탁해서 전화를 걸어달라고 했는데 오버레이, 알림 둘 다 뜨지 않고 있어. 근데 정작 표시 테스트로는 잘 뜨는데, 뭔가 연결이 안되거나 하는 것 같아. 확인하고 수정해줘.

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

- `android/app/src/main/AndroidManifest.xml`
  - `android.permission.READ_CONTACTS` 권한을 추가했다.
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/telecom/BusinessCardCallScreeningService.kt`
  - 수신 전화만 처리하도록 분기했다.
  - `respondToCall`을 `finally`에서 호출해 platform 응답 요구사항을 충족하도록 했다.
  - 수신 번호를 서비스 내부에서 직접 명함 DB와 매칭하도록 했다.
  - 설정된 표시 방식이 알림이거나 오버레이 권한이 없으면 알림을 직접 표시하도록 했다.
  - 오버레이 서비스 시작 실패 시 알림으로 fallback하도록 했다.
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/MainActivity.kt`
  - 이미 기본 앱으로 설정된 상태에서도 기본 앱 설정 화면을 열도록 변경했다.
  - 연락처 접근 권한 상태와 권한 허용 버튼을 설정 화면에 추가했다.
  - 앱 시작 시 알림/연락처 런타임 권한을 함께 요청하도록 했다.
  - 권한 결과 후 화면 상태를 다시 렌더링하도록 했다.
- `devlog.md`
  - 2026-06-10 작업 요약 행을 추가했다.
- `devlog/2026-06-10/006-android-call-screening-incoming-display-fix.md`
  - 작업 상세 기록을 추가했다.

## 참고한 공식 문서

- Android Developers `CallScreeningService`: 수신 콜에 대해 `respondToCall`을 5초 안에 호출해야 하며, 그렇지 않으면 framework가 서비스를 unbind하고 응답을 무시한다고 설명한다.

## 검증 결과

- `./gradlew :app:assembleDebug` 성공
  - Kotlin 컴파일 및 debug APK 패키징 완료
  - 기존과 동일하게 `statusBarColor`, `navigationBarColor` deprecated 경고 2건이 있으나 빌드는 정상 완료
- APK 다운로드 라우트 확인
  - `curl` 응답: `200 application/vnd.android.package-archive 2603137`
  - 다운로드 파일과 새 debug APK SHA256 일치
  - SHA256: `965a4d4e73ba581fce964a3c6a0a378cb93c83ad3ff0b12295832fd6eb8fd5d8`

## 남은 리스크

- 실제 갤럭시 폴드7 One UI 8.5 기기에서 수신 전화 이벤트가 `CallScreeningService`로 전달되는지는 단말 테스트가 필요하다.
- 사용자가 연락처 접근 권한을 거부하거나 알림 채널을 차단하면 일부 수신 표시가 동작하지 않을 수 있다.
- 제조사별 기본 앱/스팸 앱 정책에 따라 call screening 콜백 전달 조건이 다를 수 있다.
