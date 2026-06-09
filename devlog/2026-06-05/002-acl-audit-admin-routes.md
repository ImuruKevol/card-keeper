# 002. 일반 사용자 ACL 점검 및 앱 셸 API 보호 강화

## 사용자 원본 요청

- 리뷰 ID: `ugciwsakqpxvttneajljkaywqryktzfd`
- 요청 내용: 일반 계정은 다른 메뉴에 URL을 직접 치고 들어가거나 API 호출에 대해서도 확실하게 acl이 service.auth, wiz controller 형태로 걸려있는지 확인해줘.

## 확인 사항

- `/cards`는 `page.cards`의 `controller: user`와 `service.auth.allow('/access')`가 적용되어 있다.
- `/users`는 `page.users`의 `controller: admin`과 `service.auth.allow.role('admin', '/cards')`가 적용되어 있다.
- `/ai-settings`는 `page.ai_settings`의 `controller: admin`과 `service.auth.allow.role('admin', '/cards')`가 적용되어 있다.
- `page.cards` API는 `controller: user`와 `_current_user_id()`를 함께 사용하고, `struct.business_card`에서 소유자/관리자 기준으로 데이터 접근을 제한한다.
- `page.users`와 `page.ai_settings` API는 각 앱의 `controller: admin`으로 보호된다.
- 공개 라우트는 `/access`, `/auth/*`, `/manifest.json`, `/sw.js`로 확인했다.

## 변경 사항

- 공통 앱 셸 `layout.sidebar`의 controller를 `base`에서 `user`로 변경해 비밀번호 변경 API가 WIZ user controller ACL을 반드시 통과하도록 강화했다.
- 앱 셸 초기화 시 `service.auth.allow('/access')`를 호출하도록 추가해 프론트엔드 ACL도 명시했다.

## 변경 파일

- `src/app/layout.sidebar/app.json`
- `src/app/layout.sidebar/view.ts`
- `devlog.md`
- `devlog/2026-06-05/002-acl-audit-admin-routes.md`

## 검증 결과

- `wiz_project_build(clean=false)`: 성공
- `npm run test:e2e -- --project=chromium`: 성공, 3 passed
- Playwright 직접 URL 점검: 비로그인 상태에서 `/cards`, `/users`, `/ai-settings` 모두 `/access`로 이동 확인
- 직접 API 호출 점검: 쿠키 `season-wiz-project=main`, `season-wiz-devmode=true`를 포함하고 세션 없는 상태에서 아래 API 모두 `code: 401` 확인
  - `/wiz/api/page.users/list`
  - `/wiz/api/page.ai_settings/get_setting`
  - `/wiz/api/page.cards/list`
  - `/wiz/api/layout.sidebar/change_password`

## 남은 리스크

- 실제 일반 사용자 계정 세션을 확보하지 못해 로그인된 일반 사용자로 `/users`, `/ai-settings` 접근을 브라우저에서 직접 검증하지는 못했다. 단, 두 페이지와 API는 `admin` WIZ controller에 묶여 있어 일반 사용자 세션도 서버 측에서 차단된다.
