# 013. 내 명함 입력·이미지 공유·공개 링크 기능 추가

## 사용자 원래 요청

- 리뷰 ID: `wjbpgckfrfthubxlobytnfolafkprpae`
- 제목: 내 명함 기능 추가
- 요청 내용: 내 정보를 입력하고 명함의 형태로 보여주는 기능을 추가해줘. 명함 베이스 이미지는 예쁘게 잘 만들어주고, 가능하면 메인 색상, 강조 색상은 직접 선택할 수 있으면 좋을 것 같아. 만들어진 내 명함은 이미지 형태로 공유할 수 있는 기능, 내 명함 이미지를 공개 공유 링크 형태로 만들 수 있는 기능도 필요해.

## 변경 파일

- `src/app/page.my_card/app.json`
- `src/app/page.my_card/api.py`
- `src/app/page.my_card/view.pug`
- `src/app/page.my_card/view.scss`
- `src/app/page.my_card/view.ts`
- `src/app/layout.sidebar/view.pug`
- `src/app/layout.sidebar/view.scss`
- `src/model/db/my_card.py`
- `src/model/struct/my_card.py`
- `src/model/struct.py`
- `src/route/my-card-share/app.json`
- `src/route/my-card-share/controller.py`
- `devlog.md`
- `devlog/2026-06-09/013-my-card-share.md`

## 작업 내용

- `/my-card` 페이지를 추가해 이름, 회사, 부서, 직책, 연락처, 주소, 웹사이트, 한 줄 소개를 입력하고 즉시 명함 형태로 미리 볼 수 있게 했다.
- 메인 색상과 강조 색상을 직접 고르는 컬러 입력, 색상 프리셋, 3가지 명함 베이스 스타일을 추가했다.
- Canvas 기반 PNG 렌더링으로 이미지 저장 및 Web Share API 기반 이미지 공유를 지원했다.
- `my_card` DB/Struct를 추가해 사용자별 내 명함 정보, 생성 이미지, 공개 링크 토큰, 공개 상태를 저장하도록 했다.
- `/share/my-card/<token>` 공개 route를 추가해 공개 상태인 내 명함 이미지를 로그인 없이 볼 수 있게 했다.
- 앱 상단 메뉴에 `내 명함` 진입점을 추가하고 모바일 메뉴 overflow를 보강했다.

## 검증 결과

- `python -m py_compile project/main/src/model/db/my_card.py project/main/src/model/struct/my_card.py project/main/src/app/page.my_card/api.py project/main/src/route/my-card-share/controller.py` 성공.
- `git diff --check` 성공.
- `wiz_project_build(clean=true)` 성공.
- 후속 수정 후 `wiz_project_build(clean=false)` 성공.
- `npm run test:e2e` 성공: Chromium/모바일 Chromium 6개 smoke 테스트 통과.
- Playwright 직접 확인: `season-wiz-project=main`, `season-wiz-devmode=true` 쿠키를 넣고 `/my-card` 비로그인 접근 시 `/access`로 이동 확인.
- curl 확인: 동일 쿠키로 `/wiz/api/page.my_card/load` 비로그인 호출 시 `code: 401` 확인.
