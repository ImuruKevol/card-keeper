# 004. 비밀번호 변경, 관리자 권한 보호, Playwright 브라우저 테스트 구성

## 사용자 원 요청

```text
- 본인의 패스워드를 변경하는 기능이 필요함
- 사용자 관리에서 관리자를 사용자로 변경할 때 남은 관리자가 한 명도 없거나 맨 처음 초기 관리자는 사용자로 변경하지 못하도록 방어 기제를 추가할 것
- playwright 환경을 구성해서 검증 및 테스트 시 브라우저 테스트를 진행할 수 있도록 할 것
```

## 변경 요약

- 상단 앱바에 본인 비밀번호 변경 버튼과 모달을 추가했다.
- `layout.sidebar` API에 `change_password`를 추가하고, 현재 비밀번호 검증/새 비밀번호 확인/8자 이상 검증을 적용했다.
- 비밀번호 변경 성공 시 현재 세션을 제외한 같은 사용자 계정의 활성 세션을 만료하도록 했다.
- 사용자 권한 변경은 UI뿐 아니라 `struct.user.set_role()` 모델 계층에서 최종 방어하도록 보강했다.
- 초기 관리자(`initial administrator`)는 사용자 권한으로 변경할 수 없고, 마지막 활성 관리자의 사용자 권한 강등도 차단한다.
- 사용자 관리 화면에서 보호 대상 관리자 권한 셀렉트를 비활성화하고 사유를 표시한다.
- Playwright Chromium 환경을 구성하고 기본 smoke test를 추가했다.
- README에 비밀번호 변경 API, 관리자 보호 조건, Playwright 실행 명령을 문서화했다.

## 변경 파일

- `README.md`
- `.gitignore`
- `package.json`
- `playwright.config.ts`
- `tests/e2e/smoke.spec.ts`
- `src/app/layout.sidebar/api.py`
- `src/app/layout.sidebar/view.pug`
- `src/app/layout.sidebar/view.ts`
- `src/app/page.users/api.py`
- `src/app/page.users/view.pug`
- `src/app/page.users/view.ts`
- `src/model/struct/user.py`
- `devlog.md`
- `devlog/2026-06-04/004-password-admin-playwright.md`

## 검증 결과

- `python3 -m py_compile src/app/layout.sidebar/api.py src/app/page.users/api.py src/model/struct/user.py`: 통과
- `npm ls @playwright/test`: `@playwright/test@1.60.0` 확인
- `npx playwright --version`: `Version 1.60.0` 확인
- `npx playwright install chromium`: Chromium 브라우저 설치 완료
- 최초 `npm run test:e2e`: 시스템 라이브러리 `libglib-2.0.so.0` 누락으로 Chromium 실행 실패
- `npx playwright install-deps chromium`: Chromium 실행 의존성 설치 완료
- 재실행 `npm run test:e2e`: 3개 smoke test 모두 통과
- `wiz_project_build(clean=true, projectName="main")`: 성공
- `https://bus.sub.nanoha.kr/cards` + devmode 쿠키: HTTP 200
- `https://bus.sub.nanoha.kr/users` + devmode 쿠키: HTTP 200
- `https://bus.sub.nanoha.kr/wiz/api/layout.sidebar/change_password` 비로그인 호출: HTTP 200, WIZ 본문 `code: 401`, `로그인이 필요합니다.` 확인
- `https://bus.sub.nanoha.kr/wiz/api/page.users/update_role` 비로그인 호출: HTTP 200, WIZ 본문 `code: 401` 확인

## 남은 리스크

- 실제 로그인 세션이 없어 비밀번호 변경 성공 경로와 관리자 권한 강등 차단 성공 경로는 실데이터로 실행하지 못했다.
- WIZ 빌드 중 생성된 Angular 빌드 의존성에서는 기존과 같은 moderate 취약점 5건 경고가 표시된다.
