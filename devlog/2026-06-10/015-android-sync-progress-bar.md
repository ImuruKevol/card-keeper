# Android 동기화 진행률 표시 추가

- **ID**: 015
- **날짜**: 2026-06-10
- **유형**: UX 개선

## 작업 요약

동기화 중 사용자가 마냥 기다리지 않도록 Android 앱에 진행률 패널을 추가했다. 서버 변경사항을 확인하는 단계는 indeterminate progress로 표시하고, 동기화할 명함 수를 받은 뒤에는 현재/전체 건수와 퍼센트가 표시되는 progress bar로 전환되도록 구현했다. 로그인 후 동기화, 지금 동기화, 이미지 다시 동기화, 표시 테스트 자동 이미지 재동기화 흐름이 모두 동일한 진행률 표시를 사용한다.

## 원문 요청사항

```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

동기화 시 마냥 기다리게 하지 말고 progress bar같은걸 추가해서 진행률을 알 수 있도록 해줘

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
  - `SyncProgress` 모델을 추가하고 동기화 단계별 진행률 콜백을 호출하도록 변경했다.
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/MainActivity.kt`
  - 상태 배너 아래에 progress panel과 horizontal progress bar를 추가했다.
  - 로그인/동기화/이미지 재동기화/표시 테스트 자동 재동기화에 진행률 UI를 연결했다.
- `devlog.md`
  - 2026-06-10 작업 요약 행을 추가했다.
- `devlog/2026-06-10/015-android-sync-progress-bar.md`
  - 작업 상세 기록을 추가했다.

## 검증 결과

- `./gradlew :app:assembleDebug` 성공
  - debug APK 패키징 완료
  - `statusBarColor`, `navigationBarColor` deprecation 경고가 있으나 빌드는 정상 완료
- APK 다운로드 라우트 확인
  - `curl` 응답: `200 application/vnd.android.package-archive 2743815`
  - 다운로드 파일과 새 debug APK SHA256 일치
  - SHA256: `d10d281bf21548a585b1aba281a1e370074149ab1c2c327fd7b82ec8b52f7835`

## 남은 리스크

- 서버에서 동기화 대상 목록을 받기 전에는 전체 건수를 알 수 없어 indeterminate progress로 표시한다.
- 실제 기기에서 긴 이미지 동기화 중 progress bar 애니메이션과 레이아웃은 이 환경에서 직접 확인할 수 없다.
