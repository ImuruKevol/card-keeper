# 001. 상단 관리자 메뉴 노출 조건 및 AI OCR 메뉴 라벨 정리

## 사용자 원본 요청

- 리뷰 ID: `ugciwsakqpxvttneajljkaywqryktzfd`
- 제목: nav 메뉴 텍스트 에러 및 개선
- 요청 내용:
  - 상단 메뉴에 한글이 아니라 "nav.aiSettings"라고 표시되고 있으니 수정해줘.
  - 관리자가 아닌 일반 사용자는 상단 메뉴가 보일 필요 없어.

## 변경 사항

- 공통 상단 앱 셸의 메뉴 영역을 관리자 권한 사용자에게만 렌더링하도록 변경했다.
- 상단 메뉴 라벨을 번역 파이프 의존 없이 직접 한글 텍스트로 렌더링해 `nav.aiSettings` 키 노출을 방지했다.
- 관리자 메뉴 접근성 라벨을 `관리자 메뉴`로 조정했다.

## 변경 파일

- `src/app/layout.sidebar/view.pug`
- `devlog.md`
- `devlog/2026-06-05/001-nav-menu-admin-visibility.md`

## 검증 결과

- `wiz_project_build(clean=false)`: 성공
- `npm run test:e2e -- --project=chromium`: 성공, 3 passed
- `rg -n "nav\.aiSettings|AI OCR 설정|관리자 메뉴|app-nav" build/dist src/app/layout.sidebar/view.pug src/assets/lang/ko.json`: 빌드 결과에 관리자 메뉴 조건과 `AI OCR 설정` 라벨 반영 확인

## 남은 리스크

- 로그인 가능한 실제 일반/관리자 계정 정보가 없어 역할별 화면을 브라우저에서 직접 시각 검증하지는 못했다.
