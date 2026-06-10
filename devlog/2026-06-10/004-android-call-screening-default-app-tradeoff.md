# Android 발신자 표시 기본 앱 제약 안내 보강

- **ID**: 004
- **날짜**: 2026-06-10
- **유형**: UX 개선

## 작업 요약

Android call screening 기능은 시스템의 `ROLE_CALL_SCREENING` role holder가 되어야 수신 전화 콜백을 받을 수 있으므로, 후스콜 같은 기존 스팸 표시 앱과 기본 앱 역할을 동시에 나눠 쓰기 어렵다는 제약을 확인했다. 앱 권한 화면에서 이 tradeoff가 명확히 보이도록 발신자 표시 권한 문구를 `발신자 표시 기본 앱`과 `기본 앱으로 설정`으로 바꾸고, 기존 스팸 표시 앱을 유지하려면 설정을 건너뛰라는 안내를 추가했다.

## 원문 요청사항

```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

발신자 표시 권한은 반드시 기본 앱으로 설정을 해야해?
지금 후스콜 앱을 사용하고 있어서 이걸 기본 앱으로 설정하면 스팸 정보를 사용하지 못하게 되는데...

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

- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/MainActivity.kt`
  - 권한 섹션의 `발신자 표시` 라벨을 `발신자 표시 기본 앱`으로 변경했다.
  - 버튼 문구를 `발신자 표시 권한 설정`에서 `기본 앱으로 설정`으로 변경했다.
  - 후스콜 등 기존 스팸 표시 앱과 동시에 기본 앱으로 사용할 수 없다는 안내 문구를 추가했다.
  - 보조 안내 텍스트용 `supportText` helper를 추가했다.
- `devlog.md`
  - 2026-06-10 작업 요약 행을 추가했다.
- `devlog/2026-06-10/004-android-call-screening-default-app-tradeoff.md`
  - 작업 상세 기록을 추가했다.

## 참고한 공식 문서

- Android Developers `RoleManager`: role holder가 되어야 role별 권한을 얻으며, 사용자 동의로 role holder가 된다고 설명한다.
- Android Developers `CallScreeningService`: 시스템은 사용자가 선택한 `ROLE_CALL_SCREENING` role holder 앱의 `CallScreeningService`에 바인딩한다고 설명한다.

## 검증 결과

- `./gradlew :app:assembleDebug` 성공
  - Kotlin 컴파일 및 debug APK 패키징 완료
  - 기존과 동일하게 `statusBarColor`, `navigationBarColor` deprecated 경고 2건이 있으나 빌드는 정상 완료
- APK 다운로드 라우트 확인
  - `curl` 응답: `200 application/vnd.android.package-archive 2614305`
  - 다운로드 파일과 새 debug APK SHA256 일치
  - SHA256: `778dd9b8e8a4d19a1000e76660de6f4cd2cda5b4ccba5766a12598a1f15e8707`

## 남은 리스크

- 후스콜을 기본 발신자 표시/스팸 앱으로 유지하면 이 앱은 수신 전화의 자동 명함 오버레이 콜백을 받을 수 없다.
- 제조사/OS에 따라 기본 앱 설정 화면 명칭과 진입 경로가 다를 수 있어 실제 단말 확인이 필요하다.
