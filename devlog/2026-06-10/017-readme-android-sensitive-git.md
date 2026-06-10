# Android 앱 README 반영 및 커밋 전 민감정보 점검

- **ID**: 017
- **날짜**: 2026-06-10
- **유형**: 문서 업데이트

## 작업 요약

Android 네이티브 앱의 빌드 위치, 모바일 API, 수신 전화 명함 표시 기능, APK 다운로드 경로를 한국어/영어 README에 반영했다.
커밋 전 추적/미추적 커밋 대상 파일에 대해 민감정보 패턴을 점검하고 WIZ/Android 빌드를 확인했다.

## 원문 요청사항

```text
작업 시작

안드로이드 앱을 추가했는데, 이에 대한 내용을 readme에 추가해줘.
그리고 git commit & push를 하기 전 민감 정보가 있는지 확인하고 필터링한 후 commit을 해줘.
```

## 변경 파일 목록

### README

- `README.md`: Android 앱 개요, `/my-card` APK 다운로드, 모바일 API, Android 프로젝트 구조를 추가.
- `README.en.md`: 한국어 README와 같은 Android 앱 내용을 영어 문서에 반영.
- `android/README.md`: 실제 구현된 수신 전화 fallback, 알림 표시, 최근 통화/SMS 이력, 배치 오버레이 이미지 API 설명을 보강.

### Devlog

- `devlog.md`: 2026-06-10 017 작업 요약 행 추가.
- `devlog/2026-06-10/017-readme-android-sensitive-git.md`: 상세 작업 이력 추가.

## 검증 결과

- `git diff --check`: 통과.
- 커밋 대상 파일 민감정보 스캔: 356개 텍스트 파일 검사, hardcoded secret/key/token 패턴 발견 없음.
- `wiz_project_build(clean=false)`: 성공.
- `android ./gradlew :app:assembleDebug`: 성공.

## 비고

- `android/.android-sdk/`, `android/.gradle/`, `android/local.properties`, `android/**/build/`, APK/AAB 산출물은 `.gitignore`에 의해 커밋 대상에서 제외됨을 확인했다.
