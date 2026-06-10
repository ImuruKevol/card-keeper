# Android 이미지 배치 동기화 및 생성 이미지 fallback 제거

- **ID**: 016
- **날짜**: 2026-06-10
- **유형**: 버그 수정 / 성능 개선

## 작업 요약

이미지 캐시 복구 과정에서 명함 이미지가 카드별 HTTP 요청으로 내려가 동기화가 느려진 문제를 수정했다. 모바일 API에 이미지 배치 다운로드 endpoint를 추가하고 Android 동기화가 필요한 이미지 목록을 한 번에 요청하도록 변경했다. 또한 실제 업로드 사진 캐시가 없거나 잘못된 경우 알림에서 생성 명함 이미지를 fallback으로 보여주던 동작을 제거해, 전화 수신 시 이전의 기본 생성 이미지가 계속 표시되는 문제를 숨기지 않도록 했다. 이미지 캐시 버전을 올려 새 APK에서 한 번 다시 배치 검증이 수행되도록 했다.

## 원문 요청사항

```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

일단 동기화 로직이 너무 개판으로 변했어. 기존에는 그래도 뭉텅이 단위로 가져와서 속도는 빨랐는데, 지금은 한명씩 가져와서 속도가 너무 느려졌어.
그리고 전화가 왔을 때 명함 이미지 띄우는 로직 똑바로 안할래? 내가 회사 전화로 내 핸드폰에 전화를 거니까 계속 이전의 기본 생성된 이미지가 표시되고 있잖아. 앱을 완전히 삭제해야하면 확실히 말하고, 로직이 잘못되었으면 확실하게 분석해줘.

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

- `src/route/mobile-api/controller.py`
  - `/api/mobile/cards/overlay-images` POST 배치 이미지 다운로드 API를 추가했다.
  - 여러 `card_id/kind` 요청을 받아 base64 이미지 목록을 한 번에 반환하도록 했다.
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/network/MobileApiClient.kt`
  - 배치 이미지 요청/응답 모델과 JSON POST 클라이언트를 추가했다.
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/sync/BusinessCardSyncManager.kt`
  - 카드별 이미지 HTTP 다운로드를 제거하고 필요한 이미지 목록을 배치로 요청하도록 변경했다.
  - 이미지 캐시 버전을 `3`으로 올려 새 APK에서 한 번 다시 전체 이미지 검증이 수행되도록 했다.
  - 진행률 문구를 개인 이름 단위가 아니라 데이터 저장/이미지 배치 다운로드/이미지 저장 단계로 표시하도록 조정했다.
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/overlay/BusinessCardNotification.kt`
  - 실제 이미지 파일이 없거나 디코딩 실패하면 생성 명함 이미지를 대신 표시하지 않고 텍스트 알림으로 표시하도록 변경했다.
- `devlog.md`
  - 2026-06-10 작업 요약 행을 추가했다.
- `devlog/2026-06-10/016-android-batch-image-sync-notification-fallback.md`
  - 작업 상세 기록을 추가했다.

## 검증 결과

- `python -m py_compile src/route/mobile-api/controller.py src/model/struct/business_card.py` 성공
- `wiz_project_build(projectName="main", clean=false)` 성공
- 배치 API 라우트 확인
  - 인증 없는 요청 응답: `{"code": 401, "data": {"message": "모바일 인증이 필요합니다."}}`
  - 404가 아닌 인증 단계까지 도달함을 확인
- `./gradlew :app:assembleDebug` 성공
  - debug APK 패키징 완료
  - notification priority deprecation 경고가 있으나 빌드는 정상 완료
- APK 다운로드 라우트 확인
  - `curl` 응답: `200 application/vnd.android.package-archive 2743847`
  - 다운로드 파일과 새 debug APK SHA256 일치
  - SHA256: `d387a287c37660f3ffe905acc5c02eec0a96bbcc01406fca12e590336efcaddb`

## 남은 리스크

- 앱 삭제는 필요 없도록 수정했지만, 새 APK 설치 후 `이미지 다시 동기화` 또는 `지금 동기화`를 한 번 실행해야 캐시 버전 3 검증이 수행된다.
- 업로드 사진이 서버에 유효한 `front_image/back_image` data URL로 저장되어 있지 않으면 알림에는 사진 대신 텍스트만 표시된다.
- 실제 계정/실기기에서 회사 전화 수신 시 사진 표시 결과는 이 환경에서 직접 확인할 수 없다.
