# cropper 조작 중 깜빡임 안정화

- **ID**: 001
- **날짜**: 2026-06-15
- **유형**: 버그 수정

## 작업 요약
명함 촬영 후 cropper에서 드래그/확대/축소 조작이 Angular change detection을 계속 유발하지 않도록 고빈도 입력 처리를 네이티브 이벤트로 분리했다. 이미지 transform 반영을 requestAnimationFrame으로 묶고 레이아웃 값을 캐시해 조작 중 반복 레이아웃 측정과 repaint 부담을 줄였다.

## 원문 요청사항
```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

작업 시작

## 리뷰 요약

- 리뷰 ID: hhvzjpzvkwzscndvbjettkwjlbpumrwq
- 제목: 명함 촬영 후 깜빡거림
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

## 에이전트 작업 지시서

# 에이전트 작업 지시서

## 리뷰 정보

- 리뷰 ID: hhvzjpzvkwzscndvbjettkwjlbpumrwq
- 제목: 명함 촬영 후 깜빡거림
- 상태: open
- 우선순위: normal
- 분류: bug
- 프로젝트: 명함 관리 서비스
- 프로젝트 종류: web_service
- 요청 링크: https://bus.sub.nanoha.kr/
- 화면: 1440x900
- 캡처 방식: browser-display-capture-element
- 스크린샷 첨부: yes
- 리뷰 첨부 파일: 0개

## 리뷰어 요청 내용

명함 촬영 후 이미지를 자르고 확대/축소로 명함을 딱 맞게 가공하는 과정에서 내가 조작을 하면 화면이 계속 깜빡거리는 문제가 있어.

## 첨부 파일

-

## 콘솔 로그 요약

-

## 네트워크 로그 요약

-

## 환경 로그 요약

- browser-fingerprint: MacIntel / ko-KR / 2560x1440
- iframe-fingerprint: restricted / https://bus.sub.nanoha.kr
- reviewops-sdk: SDK missing
- iframe-fingerprint: restricted / https://bus.sub.nanoha.kr
- browser-fingerprint: MacIntel / ko-KR / 2560x1440
- iframe-fingerprint: restricted / https://bus.sub.nanoha.kr
- browser-fingerprint: MacIntel / ko-KR / 2560x1440
- iframe-fingerprint: restricted / https://bus.sub.nanoha.kr
```

## 변경 파일 목록
- `src/app/page.cards/view.ts`: cropper pointer/wheel/range 이벤트를 Angular 템플릿 이벤트에서 분리해 `NgZone.runOutsideAngular()` 네이티브 이벤트로 처리하고, rAF 동기화/레이아웃 캐시/cleanup을 추가했다.
- `src/app/page.cards/view.pug`: cropper stage와 scale input의 고빈도 Angular 이벤트 바인딩을 제거했다.
- `src/app/page.cards/view.scss`: cropper stage/photo/frame에 paint containment와 3D transform/backface 숨김을 추가해 조작 중 합성 레이어 안정성을 높였다.
- `devlog.md`: 작업 요약 행을 추가했다.
- `devlog/2026-06-15/001-cropper-flicker-stability.md`: 작업 상세 기록을 추가했다.

## 검증 결과
- `wiz_project_build(clean=false, projectName="main")`: 성공
- `PLAYWRIGHT_BASE_URL=http://127.0.0.1:3000 npm run test:e2e -- --project=chromium`: 성공, 3 passed
- 빌드 산출물에서 cropper stage/range의 Angular 고빈도 이벤트 바인딩이 제거되고 native listener 경로가 생성된 것을 확인했다.

## 남은 리스크
- 실제 모바일 PWA 카메라에서 촬영 파일을 선택한 뒤 손가락 드래그/핀치에 가까운 조작까지 직접 재현하지는 못했다.
- 기존 작업 중 변경된 `devlog.md`, 모바일/Android 관련 파일 등은 이번 작업 범위 밖이라 유지했다.
