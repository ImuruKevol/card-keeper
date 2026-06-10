# Android 모바일 sync API 및 Caller MVP 구현

- **ID**: 024
- **날짜**: 2026-06-09
- **유형**: 기능 추가

## 작업 요약

Android 전화 수신 명함 오버레이 설계서를 기준으로 WIZ 모바일 sync API와 Android 로컬 동기화 MVP를 구현했다.
모바일 기기 토큰, 명함 증분 sync, 오버레이 이미지 다운로드 API를 추가하고 Android 앱에서 로그인, Keystore 토큰 저장, SQLite 캐시, 전화번호 매칭, 명함 이미지 오버레이, 알림 fallback까지 연결했다.

## 원문 요청사항

```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

작업 시작

## 리뷰 요약

- 리뷰 ID: ybauiwwadospkltesipbfwwcnfcdnqyu
- 제목: 안드로이드 앱 개발
- 요청 링크: https://bus.sub.nanoha.kr/
- Codex 요청자: 권태욱
- 프로젝트 루트: /opt/app
- Codex 세션 ID: 신규
- 스크린샷 컨텍스트: 없음
- 에이전트 작업 지시서 컨텍스트: 포함됨
- HTML 문서 생성 규칙 컨텍스트: 없음
- HTML 문서 설정 컨텍스트: 없음
- HTML 프로젝트 인스트럭션 파일: 없음
- 첨부파일 컨텍스트: 0개

## 에이전트 작업 지시서

## 리뷰어 요청 내용

안드로이드 앱 개발에 대한 방향성 문서를 작성해놨으니 참고하여 앱을 실제로 개발을 진행해줘.
```

## 변경 파일 목록

### WIZ 서버

- `src/model/db/mobile_device.py`: 모바일 기기 access/refresh token, 설정, 만료 정보 저장 테이블 추가
- `src/model/struct/mobile_device.py`: 모바일 로그인 세션 생성, access 인증, refresh 회전, 로그아웃/폐기, 오버레이 설정 저장 로직 추가
- `src/model/struct.py`: `mobile_device` Struct 등록 및 테이블 초기화 대상 추가
- `src/model/struct/business_card.py`: 모바일 sync 직렬화, 전화번호 alias, 이미지 hash, 생성 이미지 렌더러, overlay image 조회 메서드 추가
- `src/route/mobile-api/app.json`: `/api/mobile/<path:action>` 라우트 추가
- `src/route/mobile-api/controller.py`: 모바일 health/login/refresh/logout/cards sync/overlay image/settings API 추가

### Android 앱

- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/MainActivity.kt`: 로그인, 동기화, 권한 요청, 위치 프리셋, 테스트 오버레이 UI 추가
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/core/PhoneNumberNormalizer.kt`: `tel:`, 내선, 국내형/82 alias 정규화 보강
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/auth/MobileTokenStore.kt`: Android Keystore 기반 토큰 암호화 저장 추가
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/data/*`: Sync 모델과 SQLite 로컬 DB 추가
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/network/MobileApiClient.kt`: WIZ 모바일 API 클라이언트와 WIZ `{code,data}` 응답 unwrap 처리 추가
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/sync/BusinessCardSyncManager.kt`: 로그인 후 sync, token refresh, 이미지 hash 기반 캐시 다운로드 추가
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/overlay/*`: 명함 이미지 렌더러, 오버레이 설정 저장, 실제 오버레이 표시, drag 저장, 알림 fallback 추가
- `android/README.md`: 현재 구현 범위, 서버 API, 실기 실행 절차, 남은 과제 현행화

## 검증 결과

- `python3 -m compileall src/model src/route/mobile-api` 성공
- `./gradlew :app:assembleDebug` 성공
- `wiz_project_build(clean=false)` 성공
- `curl -b "season-wiz-project=main; season-wiz-devmode=true" http://localhost:3000/api/mobile/health` 응답 확인: `code=200`
- `curl -b "season-wiz-project=main; season-wiz-devmode=true" http://localhost:3000/api/mobile/cards/sync` 인증 보호 확인: 응답 payload `code=401`

## 남은 리스크

- 실제 One UI 8.5 이상 Galaxy 기기에서 `CallScreeningService`, 오버레이 z-order, 잠금화면, 듀얼심, 절전 상태 QA가 필요하다.
- WorkManager 기반 자동 sync, 캐시 용량 제한, 관리자용 모바일 기기 폐기 화면은 후속 과제로 남겼다.
- Android MVP는 현재 빌드 안정성을 위해 플랫폼 API(SQLite/HttpURLConnection)로 구현했으며, Room/Retrofit 전환 여부는 후속 단계에서 결정해야 한다.
