# Android 앱 아이콘 적용

- **ID**: 012
- **날짜**: 2026-06-10
- **유형**: 디자인 개선

## 작업 요약

Android 앱이 기본 아이콘으로 표시되는 문제를 해결하기 위해 명함 카드와 전화 표시를 모티프로 한 adaptive launcher icon을 추가하고, manifest에 일반/라운드 아이콘을 연결했다.

## 원문 요청사항

```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

앱 아이콘이 지정되지 않아서 안드로이드 기본 아이콘으로 뜨고 있어. 아이콘을 적용해줘.

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
  - `android:icon`과 `android:roundIcon`을 launcher icon 리소스로 연결했다.
- `android/app/src/main/res/values/colors.xml`
  - adaptive icon 배경색을 추가했다.
- `android/app/src/main/res/drawable/ic_launcher_foreground.xml`
  - 명함 카드와 전화 배지를 표현한 벡터 foreground를 추가했다.
- `android/app/src/main/res/drawable/ic_launcher_monochrome.xml`
  - Android themed icon용 monochrome 리소스를 추가했다.
- `android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
  - adaptive launcher icon 리소스를 추가했다.
- `android/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
  - round adaptive launcher icon 리소스를 추가했다.
- `devlog.md`
  - 2026-06-10 작업 요약 행을 추가했다.
- `devlog/2026-06-10/012-android-app-icon.md`
  - 작업 상세 기록을 추가했다.

## 검증 결과

- `./gradlew :app:assembleDebug` 성공
  - 리소스 생성/패키징 및 debug APK 빌드 완료
- APK 다운로드 라우트 확인
  - `curl` 응답: `200 application/vnd.android.package-archive 2640655`
  - 다운로드 파일과 새 debug APK SHA256 일치
  - SHA256: `d5000a888fdbccc05b36e02c34754aaac6164e78b83351131495b905c59e2094`

## 남은 리스크

- 런처별 마스킹 방식에 따라 아이콘 가장자리 크롭/스케일이 다르게 보일 수 있다.
- 실제 갤럭시 폴드7 런처에서의 최종 시각 확인은 이 환경에서 직접 수행할 수 없다.
