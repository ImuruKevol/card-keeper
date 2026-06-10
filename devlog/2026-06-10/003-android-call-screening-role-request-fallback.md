# Android 발신자 표시 권한 요청 흐름 보강

- **ID**: 003
- **날짜**: 2026-06-10
- **유형**: 버그 수정

## 작업 요약

Android 앱의 `발신자 표시 권한 설정` 버튼이 사용자가 체감할 수 있는 반응 없이 끝날 수 있던 흐름을 보강했다. `RoleManager`의 call screening role 요청을 `startActivityForResult`로 실행해 승인/취소 결과를 상태 배너에 표시하고, 권한 요청 화면을 열 수 없는 기기에서는 기본 앱 설정 화면으로 이동하도록 fallback을 추가했다.

## 원문 요청사항

```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

"발신자 표시 권한 설정" 버튼을 누르니 아무 반응이 없어. "오버레이 권한 설정"은 잘 동작해서 설정했어.

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
  - call screening role 요청을 `startActivityForResult` 기반으로 변경했다.
  - 권한 요청 결과를 `onActivityResult`에서 받아 활성화/취소 상태를 표시하도록 했다.
  - role 요청 화면을 찾지 못하거나 보안 예외가 발생하는 경우 기본 앱 설정 화면으로 이동하는 fallback을 추가했다.
  - 권한 미선택 메시지를 warning 상태 배너로 표시하도록 상태 톤 판정을 보강했다.
- `devlog.md`
  - 2026-06-10 작업 요약 행을 추가했다.
- `devlog/2026-06-10/003-android-call-screening-role-request-fallback.md`
  - 작업 상세 기록을 추가했다.

## 참고한 공식 문서

- Android Developers `RoleManager`: role holder 요청은 `createRequestRoleIntent`로 얻은 Intent를 `startActivityForResult`에 전달해 사용자 동의를 받아야 한다고 명시한다.
- AOSP `CallScreeningService` 문서: call screening role 요청 예시도 `startActivityForResult`와 결과 처리를 사용한다.

## 검증 결과

- `./gradlew :app:assembleDebug` 성공
  - Kotlin 컴파일 및 debug APK 패키징 완료
  - 기존과 동일하게 `statusBarColor`, `navigationBarColor` deprecated 경고 2건이 있으나 빌드는 정상 완료
- APK 다운로드 라우트 확인
  - `curl` 응답: `200 application/vnd.android.package-archive 2614305`
  - 다운로드 파일과 새 debug APK SHA256 일치
  - SHA256: `168ed6362fa787910cbddcdc497a6a1340efcc3bbf57c65d05e6380549abf0f0`

## 남은 리스크

- call screening role 화면 노출 여부는 기기 제조사/OS 정책에 따라 달라질 수 있어 실제 단말 테스트가 필요하다.
- fallback은 기본 앱 설정 화면까지 열어주는 방식이며, 일부 기기에서는 사용자가 직접 `발신자 표시/스팸 앱` 항목을 찾아 선택해야 할 수 있다.
