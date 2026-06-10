# Android 명함 이미지 재동기화 UX 보강

- **ID**: 014
- **날짜**: 2026-06-10
- **유형**: 기능 개선

## 작업 요약

표시 테스트에서 등록된 명함 사진 대신 생성 이미지가 보이는 경우 앱 데이터를 삭제하지 않고 복구할 수 있도록 강제 이미지 재동기화 경로를 추가했다. 설정 화면에 `이미지 다시 동기화` 버튼을 추가하고, 표시 테스트 시 첫 명함의 이미지 캐시가 비어 있으면 이미지 재동기화를 먼저 실행한 뒤 테스트 알림을 표시하도록 개선했다.

## 원문 요청사항

```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

표시 테스트 버튼을 누르니 분명히 명함장 서비스에 명함 사진을 등록을 해놨는데 명함 사진은 안뜨고 기본 생성 명함 이미지가 뜨고 있어.
이러면 혹시 데이터를 싹 삭제하고 다시 동기화를 해야하나?

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

- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/sync/BusinessCardSyncManager.kt`
  - `repairImagesNow()`를 추가해 마지막 동기화 시간과 무관하게 전체 명함 이미지 캐시를 강제 재검증/재다운로드할 수 있게 했다.
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/MainActivity.kt`
  - 동기화 카드에 `이미지 다시 동기화` 버튼을 추가했다.
  - 표시 테스트 시 첫 명함 이미지 캐시가 비어 있으면 강제 이미지 재동기화 후 알림을 표시하도록 변경했다.
- `devlog.md`
  - 2026-06-10 작업 요약 행을 추가했다.
- `devlog/2026-06-10/014-android-image-resync-test-display.md`
  - 작업 상세 기록을 추가했다.

## 검증 결과

- `./gradlew :app:assembleDebug` 성공
  - debug APK 패키징 완료
  - `statusBarColor`, `navigationBarColor` deprecation 경고가 있으나 빌드는 정상 완료
- APK 다운로드 라우트 확인
  - `curl` 응답: `200 application/vnd.android.package-archive 2743815`
  - 다운로드 파일과 새 debug APK SHA256 일치
  - SHA256: `819dde1399c8543180ccecdcf31520b0750edbf3f31c9365d840f125c40807b5`

## 남은 리스크

- 이미지 재동기화 후에도 사진이 표시되지 않으면 서버에 저장된 `front_image/back_image`가 유효한 이미지 data URL인지 확인해야 한다.
- 실제 갤럭시 폴드7에서 이미지 재동기화 후 표시 테스트 결과는 이 환경에서 직접 확인할 수 없다.
