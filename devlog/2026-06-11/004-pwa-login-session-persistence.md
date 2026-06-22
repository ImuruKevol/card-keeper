# PWA 로그인 세션 유지 안정화

- **ID**: 004
- **날짜**: 2026-06-11
- **유형**: UX 개선

## 작업 요약
PWA standalone 실행 시 로그인 상태가 끊겨 보이는 빈도를 줄이기 위해 Flask 세션을 영속 세션으로 갱신하고, 인증 확인 요청과 HTML 내비게이션 캐시 정책을 보강했다.
인증 API 응답은 브라우저/서비스워커 캐시에 남지 않도록 `no-store` 헤더를 적용했고, 서비스워커는 로그인 상태에 영향을 받는 HTML 페이지 대신 정적 자산만 캐시하도록 조정했다.

## 원문 요청사항
```text
PWA 앱에서 로그인 상태 유지가 최대한 보존될 수 있도록 개선해줘.
현재는 PWA 앱을 열면 로그인이 끊겨있을 때가 종종 있어.
```

## 변경 파일 목록
- `config/season.py`
  - 로그인 세션을 180일 영속 세션으로 설정하고 요청마다 만료 기한을 갱신하도록 보강
  - 세션 쿠키 `HttpOnly`, `SameSite=Lax`, refresh 설정 명시
- `config/pwa/sw.js`
  - 캐시 버전을 `business-card-manager-v3`로 갱신
  - `/cards`, `/access`, `/` HTML 내비게이션 캐시 제거
  - 정적 PWA 자산만 캐시하고 내비게이션은 네트워크 우선으로 처리
- `src/controller/base.py`
  - 기존 로그인 세션이 있는 요청에서 영속 세션 갱신 호출
- `src/portal/season/route/auth/controller.py`
  - `/auth/check`, `/auth/logout`, `/auth/login` 응답에 no-store 헤더 적용
- `src/portal/season/libs/src/auth.ts`
  - `/auth/check` 재시도 1회 추가
  - 인증 확인 요청에 `credentials: same-origin`, `cache: no-store` 명시
- `src/portal/season/libs/util/request.ts`
  - 공통 POST 요청 기본값에 쿠키 포함과 no-store 캐시 정책 적용
- `devlog.md`
  - 이번 작업 요약 행 추가
- `devlog/2026-06-11/004-pwa-login-session-persistence.md`
  - 이번 작업 상세 기록 추가

## 검증 결과
- `wiz_project_build(projectName="main", clean=false)` 성공.
- `python -m py_compile config/season.py src/controller/base.py src/portal/season/route/auth/controller.py` 통과.
- Flask 테스트 요청 컨텍스트에서 `session_refresh(force=True)` 호출 시 `session.permanent=True`, lifetime 180일, `SESSION_REFRESH_EACH_REQUEST=True`, `SESSION_COOKIE_SAMESITE=Lax`, `SESSION_COOKIE_HTTPONLY=True` 확인.
- 쿠키 `season-wiz-project=main; season-wiz-devmode=true`를 붙여 로컬 `http://127.0.0.1:3000/auth/check` 호출 시 HTTP 200 및 `Cache-Control: no-store, no-cache, must-revalidate, max-age=0` 확인.
- 쿠키 `season-wiz-project=main; season-wiz-devmode=true`를 붙여 로컬 `http://127.0.0.1:3000/sw.js` 호출 시 `business-card-manager-v3` 서비스워커 응답 확인.
- 별도 `npm run --prefix src/angular build -- --configuration production`은 `@angular-devkit/build-angular:browser-esbuild` 패키지 부재로 실패했으나, WIZ 빌드는 정상 완료.

## 남은 리스크
- 실제 사용자 계정으로 PWA 재실행 후 브라우저/OS별 쿠키 보존 동작까지 수동 확인하지는 않았다.
- iOS/macOS PWA의 저장소 정리 정책이나 사용자의 명시적 쿠키 삭제는 애플리케이션 코드에서 완전히 방지할 수 없다.
