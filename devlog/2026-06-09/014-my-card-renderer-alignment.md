# 014. 내 명함 미리보기·공유 이미지 렌더링 일치화

## 사용자 원래 요청

- 리뷰 ID: `wjbpgckfrfthubxlobytnfolafkprpae`
- 제목: 내 명함 기능 추가
- 요청 내용: 제공된 계정으로 실제 명함 이미지를 확인하고 수정. Preview 이미지와 이미지 공유 결과가 일치하지 않고, 왼쪽 상단의 불필요한 박스가 보이며, 모든 정보를 입력했을 때 명함 레이아웃이 깨지는 문제를 수정 요청.
- 참고: 로그인 검증용 비밀번호는 보안상 devlog에 기록하지 않음.

## 변경 파일

- `src/app/page.my_card/view.pug`
- `src/app/page.my_card/view.scss`
- `src/app/page.my_card/view.ts`
- `src/route/my-card-share/controller.py`
- `devlog.md`
- `devlog/2026-06-09/014-my-card-renderer-alignment.md`

## 작업 내용

- HTML/CSS Preview와 Canvas 공유 이미지가 따로 그려지던 구조를 visible Canvas 단일 렌더러로 통합했다.
- Preview에서 보이는 Canvas를 그대로 이미지 저장, 이미지 공유, 공개 링크 생성에 사용해 결과 이미지를 일치시켰다.
- 왼쪽 상단 이니셜 박스를 제거하고 회사명, 이름, 역할, 소개, 연락처, 주소 중심의 명함 레이아웃으로 재구성했다.
- 연락처 영역을 라벨 기반으로 정리하고 긴 웹사이트/주소는 줄바꿈·말줄임 처리되도록 Canvas 텍스트 래핑을 보강했다.
- PNG data URL이 nginx 요청 제한을 넘는 문제를 피하도록 공유/저장 이미지를 JPEG로 생성하고 공개 페이지 다운로드 확장자도 이미지 타입에 맞게 조정했다.
- 기존 공개 링크의 저장 이미지를 새 렌더러 출력으로 갱신했다.

## 검증 결과

- 제공 계정으로 로그인 후 `/my-card` 실제 화면 확인.
- 기존 저장 데이터 전체 입력 상태에서 Preview Canvas 시각 확인.
- `이미지 저장` 결과와 Preview Canvas의 JPEG data URL이 동일함을 확인.
- `공유 링크` 생성 후 공개 페이지 이미지가 Preview Canvas JPEG data URL과 동일함을 확인.
- 긴 회사명/부서/직책/웹사이트/주소/소개 입력 상태에서 Preview 레이아웃이 박스 없이 유지됨을 확인.
- 모바일 412x960 뷰포트에서 내 명함 화면과 Preview 영역을 스크린샷으로 확인.
- `python -m py_compile project/main/src/route/my-card-share/controller.py` 성공.
- `git diff --check` 성공.
- `wiz_project_build(clean=false)` 성공.
- `npm run test:e2e` 성공: Chromium/모바일 Chromium 6개 smoke 테스트 통과.
