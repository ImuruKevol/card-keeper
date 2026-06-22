# Android 미등록 명함 알림 연락처 이름 표시

- **ID**: 005
- **날짜**: 2026-06-11
- **유형**: UX 개선

## 작업 요약
Android 앱에서 수신 번호에 매칭되는 명함이 없을 때도 단말 연락처 이름을 조회해 알림 제목에 표시하도록 개선했다.
연락처 이름이 없으면 통화 기록의 캐시 이름을 보조로 확인하고, 둘 다 없을 때는 기존처럼 `등록된 명함 없음`을 표시한다.
연락처/통화 기록 권한 안내 문구도 새 표시 동작에 맞춰 보강했다.

## 원문 요청사항
```text
전화가 왔을 때 지금은 명함이 등록되어있지 않으면 알림에 그냥 없다고 뜨는데, 등록된 이름 정보가 있으면 가져와서 표시해줘.
가능하면 에이닷같은 앱을 쓸 때 번호가 등록되어있지 않아도 자동으로 이름을 표시해주는데, 이런 부분도 가져와서 표시할 수 있으면 너무 좋아.
```

## 변경 파일 목록
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/data/CallerNameResolver.kt`
  - Android 연락처 `PhoneLookup` 조회와 통화 기록 `CACHED_NAME` fallback 추가
  - 전화번호처럼 보이는 값은 이름으로 사용하지 않도록 필터링
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/data/MobileModels.kt`
  - 미등록 명함 제목 상수와 resolved name 판별 속성 추가
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/telecom/IncomingCallerDisplay.kt`
  - 명함 미등록 시 연락처/캐시 이름을 포함한 missing card 생성
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/overlay/BusinessCardNotification.kt`
  - resolved name이 있으면 알림 제목에 이름을 우선 표시
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/overlay/BusinessCardDisplayController.kt`
  - 오버레이 fallback에서도 resolved name과 미등록 상태를 함께 표시
- `android/app/src/main/kotlin/kr/nanoha/buscard/caller/MainActivity.kt`
  - 연락처/통화 기록 권한 안내 문구 갱신
- `devlog.md`
  - 이번 작업 요약 행 추가
- `devlog/2026-06-11/005-android-missing-card-contact-name.md`
  - 이번 작업 상세 기록 추가

## 검증 결과
- `./gradlew :app:assembleDebug` 성공.
- `git diff --check` 통과.

## 비고
- Android 보안 정책상 에이닷 등 다른 앱의 사설 발신자 DB를 직접 조회하지는 않았다.
- 대신 OS가 접근을 허용하는 단말 연락처와 통화 기록 캐시 이름 범위에서 best-effort로 이름을 표시한다.
