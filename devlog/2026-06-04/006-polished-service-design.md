# 006. 명함 관리 서비스 전반 세련화 디자인 정리

## 사용자 원 요청

```text
작업 시작

## 리뷰 요약

- 리뷰 ID: hfcdnrphuhohqdptaeemafirwucolqjj
- 제목: 디자인 수정
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

## 리뷰어 요청 내용

디자인이 너무 예쁘지 않아. 세련된 디자인으로 수정해줘
```

## 변경 요약

- 공통 상단 앱 셸을 브랜드 로고, 중앙 내비게이션, 사용자 칩, 정돈된 아이콘 버튼 구조로 재구성했다.
- 로그인/가입 화면을 좌측 브랜드 비주얼과 우측 인증 폼 레이아웃으로 정리하고 모바일 간격을 조정했다.
- 명함 관리 화면의 상단 검색/등록 바, 사진 등록 패널, 테이블, 모바일 목록, 사진 분석 모달을 같은 시각 체계로 정리했다.
- 사용자 관리 화면의 검색/권한 필터, 상태 세그먼트, 테이블, 모바일 카드 목록을 명함 화면과 일관된 디자인으로 맞췄다.
- 동적 상태 배지는 Tailwind 문자열 의존 대신 컴포넌트 SCSS 상태 클래스로 전환했다.
- 전역 기본 스타일에 박스 모델, body 기본 여백, 폼 폰트 상속, 렌더링 기본값을 추가했다.

## 변경 파일

- `src/angular/styles/styles.scss`
- `src/app/layout.sidebar/view.pug`
- `src/app/layout.sidebar/view.scss`
- `src/app/page.access/view.pug`
- `src/app/page.access/view.scss`
- `src/app/page.cards/view.pug`
- `src/app/page.cards/view.scss`
- `src/app/page.cards/view.ts`
- `src/app/page.users/view.pug`
- `src/app/page.users/view.scss`
- `src/app/page.users/view.ts`
- `devlog.md`
- `devlog/2026-06-04/006-polished-service-design.md`

## 검증 결과

- `wiz_project_build(clean=false, projectName="main")`: 성공
- 개발 쿠키 `season-wiz-project=main`, `season-wiz-devmode=true` 포함 로컬 브라우저 검증:
  - `http://127.0.0.1:3000/access` 데스크톱 1440x900 스크린샷 확인
  - `http://127.0.0.1:3000/access` 모바일 393x852 스크린샷 확인
- `PLAYWRIGHT_BASE_URL=http://127.0.0.1:3000 npx playwright test`: 데스크톱 Chromium 3개 + 모바일 Chromium 3개, 총 6개 smoke test 통과

## 남은 리스크

- 이번 요청에는 로그인 계정 정보가 없어 인증 후 `/cards`, `/users` 실제 데이터 화면은 빌드와 기존 smoke 범위로만 확인했다.
- 현재 git 작업트리에는 이전 작업으로 보이는 미커밋 변경이 다수 존재하므로, 이번 변경 범위 외 파일은 정리하지 않았다.
