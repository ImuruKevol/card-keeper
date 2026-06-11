# Android 스크린샷 익명화 및 README 추가

- **ID**: 001
- **날짜**: 2026-06-11
- **유형**: 문서 업데이트

## 작업 요약

ReviewOps 첨부 Android 스크린샷 3장을 README용 자산으로 추가했다.
`phone.png`, `app_alarm.png`에 보이는 이름, 전화번호, 이메일, 주소 등 개인 식별 가능 영역은 더미 정보로 덮어쓴 파생 이미지만 저장했다.

## 원문 요청사항

```text
스크린샷도 첨부했으니 readme에 추가해줘.
app_alarm.png, phone.png 상단에 내 개인정보가 들어가있으니 다른 이름(ex: 홍길동)으로 바꾸고 번호도 바꿔야해
```

## 변경 파일 목록

### README

- `README.md`: Android 앱 설정, Android 알림, 전화 수신 화면 스크린샷 표 추가 및 익명화 설명 갱신.
- `README.en.md`: 동일한 Android 스크린샷 표와 익명화 설명을 영어 문서에 반영.

### 이미지 자산

- `docs/screenshots/android-app-settings.png`: Android 앱 설정 화면 첨부 이미지 추가.
- `docs/screenshots/android-notification-sanitized.png`: `app_alarm.png`의 이름/연락처/명함 이미지 영역을 더미 정보로 익명화한 파생 이미지 추가.
- `docs/screenshots/android-phone-sanitized.png`: `phone.png`의 이름/연락처/명함 이미지 영역을 더미 정보로 익명화한 파생 이미지 추가.

### Devlog

- `devlog.md`: 2026-06-11 001 작업 요약 행 추가.
- `devlog/2026-06-11/001-readme-android-screenshots.md`: 상세 작업 이력 추가.

## 검증 결과

- 생성된 Android 스크린샷 3장을 시각 확인했다.
- 익명화 이미지에는 더미 이름 `홍길동`, 더미 번호 `010-1234-5678` 및 예시 이메일/주소만 남도록 확인했다.
- README의 이미지 링크 대상 파일 존재 여부를 확인했다.
- `git diff --check` 통과.

## 비고

- 원본 첨부 이미지는 프로젝트에 저장하지 않았다.
- 생성형 이미지 편집 대신 원본 UI 보존을 위해 로컬 이미지 후처리로 개인정보 영역만 덮어썼다.
