# Android 업로드 명함 이미지 표시 오류 수정

- **ID**: 013
- **날짜**: 2026-06-10
- **유형**: 버그 수정

## 작업 요약

Android 알림에 사용자가 촬영/업로드한 명함 사진 대신 생성된 기본 명함 이미지가 표시될 수 있는 문제를 수정했다. 서버가 요청한 앞/뒷면 이미지를 해석하지 못할 때 생성 이미지로 조용히 fallback하지 않도록 막고, Android 동기화는 기대한 이미지 해시와 실제 응답 해시가 다르면 캐시를 저장하지 않고 기존 캐시를 삭제하도록 보강했다. 기존 잘못된 이미지 캐시를 복구하기 위해 이번 이미지 캐시 버전에서는 한 번 전체 명함을 다시 동기화하도록 했다.

## 원문 요청사항

```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

명함 이미지가 내가 촬영해서 업로드한 명함 사진이 아니라 다른 기본? 명함 이미지가 뜨는 버그가 있어.

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

- `src/model/struct/business_card.py`
  - 앞/뒷면 이미지가 유효한 data URL일 때만 모바일 동기화 이미지 종류와 해시를 노출하도록 수정했다.
  - 요청한 `front/back` 이미지를 디코딩할 수 없으면 생성 이미지 fallback 대신 404가 되도록 수정했다.
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/data/BusinessCardDatabase.kt`
  - 잘못된 이미지 캐시 행을 제거할 수 있는 `deleteOverlayImage`를 추가했다.
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/sync/BusinessCardSyncManager.kt`
  - 이미지 응답 해시가 기대 해시와 다르면 캐시를 삭제하고 저장하지 않도록 수정했다.
  - 이미지 캐시 복구 버전에서 한 번 전체 명함을 강제 동기화하고 이미지를 다시 검증하도록 추가했다.
- `devlog.md`
  - 2026-06-10 작업 요약 행을 추가했다.
- `devlog/2026-06-10/013-android-uploaded-card-image-cache-fix.md`
  - 작업 상세 기록을 추가했다.

## 검증 결과

- `python -m py_compile src/model/struct/business_card.py` 성공
- `./gradlew :app:assembleDebug` 성공
  - debug APK 패키징 완료
  - `statusBarColor`, `navigationBarColor` deprecation 경고가 있으나 빌드는 정상 완료
- APK 다운로드 라우트 확인
  - `curl` 응답: `200 application/vnd.android.package-archive 2743815`
  - 다운로드 파일과 새 debug APK SHA256 일치
  - SHA256: `75770184965373bc8e16caead509d0df91a3df8d5f474e51100f3ee026c90cd8`

## 남은 리스크

- 기존 앱 사용자는 새 APK 설치 후 `지금 동기화`를 실행해야 잘못 저장된 이미지 캐시가 복구된다.
- 업로드된 `front_image/back_image` 자체가 유효한 data URL이 아니면 Android에서는 촬영 이미지 대신 생성 이미지가 표시될 수 있다.
- 실제 계정의 촬영 이미지 표시 결과는 이 환경에서 직접 확인할 수 없다.
