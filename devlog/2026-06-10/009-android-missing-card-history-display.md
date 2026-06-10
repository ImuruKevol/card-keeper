# Android 명함 없음 상태 최근 기록 표시

- **ID**: 009
- **날짜**: 2026-06-10
- **유형**: 기능 개선

## 작업 요약

수신 번호와 매칭되는 등록 명함이 없어도 표시를 중단하지 않고 `등록된 명함 없음` 상태를 보여주도록 개선했다. 명함 없음 상태에서는 명함 이미지를 생성하지 않고, 오버레이/알림에 번호와 최근 통화/문자 기록만 표시하도록 분기했다.

## 원문 요청사항

```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

등록된 명함 정보가 없으면 "등록된 명함 없음" 표시와 함께 최근 기록들만 표시하면 돼.

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
- HTML 프로젝트 인스트럭션 파일: 없음
- 첨부파일 컨텍스트: 0개
```

## 변경 파일 목록

- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/data/MobileModels.kt`
  - `CachedBusinessCard`에 명함 없음 placeholder 식별자를 추가했다.
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/telecom/BusinessCardCallScreeningService.kt`
  - DB 매칭 결과가 없을 때 `등록된 명함 없음` placeholder를 생성해 표시 흐름을 이어가도록 했다.
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/overlay/BusinessCardDisplayController.kt`
  - 명함 없음 상태에서는 명함 이미지 대신 제목/번호/최근 기록만 표시하도록 오버레이 렌더링을 분기했다.
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/overlay/BusinessCardNotification.kt`
  - 명함 없음 상태에서는 BigText 알림으로 최근 기록 중심 표시를 하도록 분기했다.
- `devlog.md`
  - 2026-06-10 작업 요약 행을 추가했다.
- `devlog/2026-06-10/009-android-missing-card-history-display.md`
  - 작업 상세 기록을 추가했다.

## 검증 결과

- `./gradlew :app:assembleDebug` 성공
  - Kotlin 컴파일 및 debug APK 패키징 완료
  - notification priority, status/navigation bar color deprecation 경고가 있으나 빌드는 정상 완료
- APK 다운로드 라우트 확인
  - `curl` 응답: `200 application/vnd.android.package-archive 2718069`
  - 다운로드 파일과 새 debug APK SHA256 일치
  - SHA256: `f21bd3e8ce33f0125a8886712d0acfdebb7d379ee234c974abd9fdf23cead726`

## 남은 리스크

- 명함 없음 상태의 실제 최근 통화/문자 기록 노출은 통화 기록/문자 기록 권한 허용 여부에 의존한다.
- 실제 수신 번호가 비공개/알 수 없음으로 전달되면 최근 기록 매칭이 비어 있을 수 있다.
