# 비밀번호 변경 모달 변경 버튼 가시성 복구

- **ID**: 001
- **날짜**: 2026-06-08
- **리뷰 ID**: gqzhtbdjflgxeximbdzhktmzdxuxkdse
- **유형**: 버그 수정

## 사용자 원 요청

```text
작업 시작

리뷰어 요청 내용:
패스워드 모달에서 취소 버튼 옆에 변경 버튼이 보이질 않음
```

## 변경 요약

`layout.sidebar`의 패스워드 변경 다이얼로그가 `.app-shell` 바깥 형제로 렌더링되는데, 버튼 배경에 쓰는 CSS 변수는 `.app-shell` 내부에만 정의되어 있었다.
색상 토큰을 `:host`로 올려 다이얼로그와 앱 셸 모두 같은 변수를 상속받도록 수정했다.

## 변경 파일 목록

- `src/app/layout.sidebar/view.scss`
- `devlog.md`
- `devlog/2026-06-08/001-password-modal-button-visibility.md`

## 검증 결과

- `wiz_project_build(clean=false, projectName="main")`: 성공
- `npm run test:e2e -- --project=chromium`: 성공, 개발 쿠키 `season-wiz-project=main`, `season-wiz-devmode=true` 적용된 smoke test 3개 통과
- Playwright 계산 스타일 검사: `변경` 버튼 visible, 배경색 `rgb(15, 118, 110)`, 글자색 `rgb(255, 255, 255)`, 크기 `60x38` 확인

## 남은 리스크

- 실제 로그인 계정 정보가 없어 운영 화면에서 패스워드 변경 모달을 직접 열어 조작 검증하지는 못했다.
