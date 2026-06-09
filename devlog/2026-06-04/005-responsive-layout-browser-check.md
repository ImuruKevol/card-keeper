# 005. 로그인 기반 PC·모바일 레이아웃 점검 및 반응형 목록 정리

## 사용자 원 요청

```text
레이아웃이 이상해. 아래 로그인 정보를 활용해서 직접 브라우저 화면을 확인 후 레이아웃을 정리해줘. PC화면 뿐만 아니라 PWA 앱으로 사용할거라서 모바일 화면도 확인해야해.
ID: rnjsxodnr17@naver.com
PW: [마스킹]
```

## 변경 요약

- 제공된 계정으로 Playwright 브라우저 로그인을 수행해 `/cards`, `/users`, 명함 등록 모달을 데스크톱과 모바일에서 직접 확인했다.
- 데스크톱에서 페이지 콘텐츠가 좌측으로 붙는 문제를 해결하기 위해 명함/사용자 화면의 페이지 헤더와 본문 컨테이너에 `mx-auto w-full` 중앙 정렬을 적용했다.
- 모바일에서 테이블 컬럼이 좁은 화면에 눌리며 헤더 텍스트가 한 글자씩 줄바꿈되는 문제를 해결했다.
- 명함 화면은 모바일에서 테이블 대신 카드형 목록/빈 상태를 표시하도록 변경했다.
- 사용자 관리 화면도 모바일에서 테이블 대신 사용자 카드형 목록과 권한/상태/작업 영역을 표시하도록 변경했다.
- Playwright 설정에 `mobile-chromium` 프로젝트를 추가해 기본 smoke 테스트가 데스크톱과 Pixel 7 모바일에서 함께 실행되도록 했다.
- README의 Playwright 설명을 데스크톱/모바일 테스트 기준으로 갱신했다.

## 변경 파일

- `README.md`
- `playwright.config.ts`
- `src/app/page.cards/view.pug`
- `src/app/page.users/view.pug`
- `devlog.md`
- `devlog/2026-06-04/005-responsive-layout-browser-check.md`

## 검증 결과

- Playwright 로그인 브라우저 점검: 제공 계정으로 `/access` 로그인 후 `/cards` 진입 확인
- 데스크톱 `/cards`: HTTP/브라우저 렌더링 정상, 가로 오버플로우 없음, 중앙 정렬 적용 확인
- 모바일 `/cards`: 가로 오버플로우 없음, 테이블 숨김, 빈 상태 카드 표시 확인
- 모바일 `/users`: 가로 오버플로우 없음, 사용자 카드형 목록 표시 확인
- `wiz_project_build(clean=true, projectName="main")`: 성공
- `npm run test:e2e`: 데스크톱 Chromium 3개 + 모바일 Chromium 3개, 총 6개 smoke test 통과

## 남은 리스크

- 실제 명함 데이터가 없는 계정이라 명함 카드 목록의 다건 데이터 상태는 직접 확인하지 못했다.
- 명함 이미지 업로드 후 OCR 결과가 채워진 등록 모달 상태는 이번 레이아웃 검증 범위에서 실제 파일 업로드로 확인하지 않았다.
- WIZ 빌드 중 생성된 Angular 빌드 의존성에서는 기존과 같은 moderate 취약점 5건 경고가 표시된다.
