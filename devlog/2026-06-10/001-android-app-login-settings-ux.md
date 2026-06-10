# Android 앱 로그인 및 기능 설정 UX 개선

- **ID**: 001
- **날짜**: 2026-06-10
- **유형**: 디자인 개선

## 작업 요약

Android 앱의 로그인 전/후 화면을 분리하고, 로그인 상태를 배지와 상태 배너로 명확히 표시하도록 개선했다. 비밀번호 입력은 명시적으로 password transformation을 적용해 마스킹되도록 보강했고, 로그인 후에는 동기화, 권한, 오버레이 위치 설정을 카드형 설정 화면으로 정리했다.

## 원문 요청사항

```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

앱을 좀 예쁘게 디자인도 좀 하고 ui ux도 생각을 해줘... 너무 기능만 만들어졌어. 패스워드도 password타입이 아니라 그냥 쌩 텍스트 입력이고, 로그인 시 제대로 로그인이 된건지도 구분이 안가고...
로그인 화면과 기능 설정 화면이 구분도 되어야 하고...

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
  - 로그인 전 화면과 로그인 후 기능 설정 화면을 분리했다.
  - 비밀번호 입력에 `PasswordTransformationMethod`를 적용했다.
  - 로그인 상태 배지, 상태 배너, 카드형 설정 섹션, 버튼 스타일을 추가했다.
  - 로그인/동기화/로그아웃 진행 중 버튼 비활성화와 결과 메시지 표시를 보강했다.
  - 권한 상태와 마지막 동기화, 로컬 명함 수를 설정 화면에서 확인할 수 있게 했다.
- `devlog.md`
  - 2026-06-10 작업 요약 행을 추가했다.
- `devlog/2026-06-10/001-android-app-login-settings-ux.md`
  - 작업 상세 기록을 추가했다.

## 검증 결과

- `./gradlew :app:assembleDebug` 성공
  - Kotlin 컴파일 및 debug APK 패키징 완료
  - `statusBarColor`, `navigationBarColor` deprecated 경고 2건은 기존 Android API deprecation 경고이며 빌드는 정상 완료
- APK 다운로드 라우트 확인
  - `curl` 응답: `200 application/vnd.android.package-archive 2614305`
  - 다운로드 파일과 새 debug APK SHA256 일치
  - SHA256: `353d44a03473e3b44e83a1bb5ef11e65a28180f08aea11a486cd9fc290266535`

## 남은 리스크

- 실제 Android 단말에서 키보드 동작, 비밀번호 마스킹 표시, 권한 설정 왕복 후 화면 상태는 기기 테스트가 필요하다.
- 현재 앱은 XML/Compose가 아닌 programmatic View 기반이라, 향후 더 큰 디자인 개편은 전용 레이아웃 또는 Compose 전환을 검토하는 편이 유지보수에 유리하다.
