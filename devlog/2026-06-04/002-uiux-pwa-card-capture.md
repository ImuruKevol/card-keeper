# 명함 사진 분석 중심 UI/UX 정리 및 PWA 설정 보강

- **ID**: 002
- **날짜**: 2026-06-04
- **유형**: 기능 추가 / 설정 변경 / 리팩토링 / 디자인

## 작업 요약
명함 관리 서비스의 인증 후 화면을 `/cards` 명함 관리와 관리자 전용 `/users`만 남기도록 정리했다.
명함 등록 UX를 수기 입력 중심에서 사진 업로드/카메라 촬영 후 OCR 분석 결과를 검토해 저장하는 흐름으로 재구성하고, PWA manifest/service worker/아이콘/로고 설정을 보강했다.

## 원문 요청사항
```text
작업 시작

## 리뷰 요약

- 리뷰 ID: dnqcrwmpwdvgnhdnkmeujepdllcchmxp
- 제목: UI/UX 정리
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

- 리뷰 ID: dnqcrwmpwdvgnhdnkmeujepdllcchmxp
- 제목: UI/UX 정리
- 상태: open
- 우선순위: high
- 분류: design
- 프로젝트: 명함 관리 서비스
- 프로젝트 종류: web_service
- 요청 링크: https://bus.sub.nanoha.kr/
- 화면: 1440x900
- 캡처 방식: capture-unavailable-cross-origin
- 스크린샷 첨부: yes
- 리뷰 첨부 파일: 0개

## 리뷰어 요청 내용

- 이 서비스는 명함 관리 화면 외의 다른 화면은 필요 없어. 단, 관리자에 한해서는 별도로 사용자 관리 화면으로 이동할 수 있는 버튼을 어딘가에 추가하고, 사용자 관리 화면은 있어야 해.
- 명함 등록은 수동으로 사용자가 입력하는게 아니라, 명함 사진을 업로드하거나 카메라로 찍으면 그걸 분석해서 자동으로 정보를 입력해주는거야. 그에 맞게 레이아웃, UI/UX를 전면 수정해줘.
- 이 앱은 PWA 웹앱 형태로 설치해서 사용하도록 할거야. 그에 맞는 각종 설정들을 하고, 로고도  알맞게 새로 만들어줘.

## 첨부 파일

-

## 콘솔 로그 요약

-

## 네트워크 로그 요약

-

## 환경 로그 요약

- browser-fingerprint: Win32 / ko-KR / 1920x1080
- iframe-fingerprint: restricted / https://bus.sub.nanoha.kr
- iframe-fingerprint: restricted / https://bus.sub.nanoha.kr
- browser-fingerprint: Win32 / ko-KR / 1920x1080
- iframe-fingerprint: restricted / https://bus.sub.nanoha.kr
- reviewops-sdk: SDK missing
- browser-fingerprint: Win32 / ko-KR / 1920x1080
- iframe-fingerprint: restricted / https://bus.sub.nanoha.kr
```

## 변경 파일 목록

### 화면/라우팅
- `src/app/page.cards/view.ts`: 사진 파일 선택, 카메라 촬영, Tesseract.js OCR 로딩, 텍스트 파싱, 분석 상태/진행률/신뢰도, 분석 결과 저장 흐름 추가
- `src/app/page.cards/view.pug`: 명함 목록과 등록 플로우를 사진 기반 분석/검토 UI로 전면 재구성, 관리자 사용자 관리 버튼 추가
- `src/app/component.nav.sidebar/view.pug`: 사이드바 메뉴를 명함 관리와 관리자 전용 사용자 관리로 축소
- `src/app/component.nav.sidebar/view.ts`: 활성 메뉴 색상과 상태를 새 테마에 맞게 조정
- `src/app/page.access/api.py`: 로그인 성공 리다이렉트를 `/cards`로 변경
- `src/app/page.access/view.ts`: 이미 로그인된 사용자와 로그인 성공 이동 경로를 `/cards`로 변경
- `src/app/page.access/view.pug`: 기존 파란색 강조를 PWA 테마 색상으로 조정
- `src/app/page.users/view.ts`: 비관리자 접근 실패 시 `/cards`로 이동하도록 변경
- `src/app/page.users/view.pug`: 사용자 관리 화면 테마 색상 조정
- `src/app/page.dashboard/`: 삭제
- `src/app/page.mypage/`: 삭제
- `src/angular/app/app-routing.module.ts`: 기본 라우트를 `cards`로 변경
- `src/portal/season/route/auth/controller.py`: auth fallback 리다이렉트를 `/cards`로 변경

### PWA/브랜딩
- `config/season.py`: PWA 이름, 시작 URL, display/orientation, 테마 색상, 아이콘 경로 설정
- `src/angular/index.pug`: PWA 메타 태그, manifest, 아이콘 링크, service worker 등록 설정 정리
- `src/route/manifest/app.json`: `/manifest.json` Source route 추가
- `src/route/manifest/controller.py`: PWA manifest JSON 동적 응답 구현
- `config/pwa/sw.js`: 앱 shell과 정적 자산 캐시용 service worker 추가
- `src/assets/brand/logo-black.svg`: 명함장 로고 SVG 교체
- `src/assets/brand/logo-white.svg`: 명함장 로고 SVG 교체
- `src/assets/brand/icon.ico`: PWA favicon/ICO 교체
- `src/assets/brand/icon-192.png`: PWA 192px 아이콘 추가
- `src/assets/brand/icon-512.png`: PWA 512px 아이콘 추가
- `src/assets/lang/ko.json`: 메뉴 번역을 명함/사용자 관리만 남기도록 정리
- `src/assets/lang/en.json`: 메뉴 번역을 명함/사용자 관리만 남기도록 정리

### 문서
- `README.md`: `/cards` 중심 화면 구성, PWA 구성, 명함/사용자 API 설명으로 갱신
- `devlog.md`: 이번 작업 요약 행 추가
- `devlog/2026-06-04/002-uiux-pwa-card-capture.md`: 이번 작업 상세 devlog 추가

## 검증 결과
- `wiz_project_build(clean=true)`: 성공
- `wiz_project_build(clean=false)`: 성공
- 개발 쿠키 `season-wiz-project=main`, `season-wiz-devmode=true` 포함 검증:
  - `GET https://bus.sub.nanoha.kr/cards`: HTTP 200, `text/html; charset=utf-8`
  - `GET https://bus.sub.nanoha.kr/users`: HTTP 200, `text/html; charset=utf-8`
  - `GET https://bus.sub.nanoha.kr/manifest.json`: HTTP 200, `application/manifest+json; charset=utf-8`
  - `GET https://bus.sub.nanoha.kr/sw.js`: HTTP 200, `text/javascript; charset=utf-8`
  - `GET https://bus.sub.nanoha.kr/wiz/api/page.cards/list`: HTTP 200, WIZ 응답 `code=401`로 비로그인 보호 확인
- Manifest 내용 확인: `name=명함장`, `start_url=/cards`, `display=standalone`, `theme_color=#0f766e`, 아이콘 `192x192`, `512x512` 포함
- 아이콘 파일 확인: `icon-192.png` 192x192, `icon-512.png` 512x512, `icon.ico` ICO 생성 확인
- 소스/설정/README 범위에서 `/dashboard`, `/mypage`, `page.dashboard`, `page.mypage` 잔여 참조 없음

## 남은 리스크
- OCR은 브라우저에서 Tesseract.js CDN을 동적으로 로드하는 방식이므로 네트워크/CSP/CDN 장애 시 분석 엔진을 사용할 수 없다.
- 실제 명함 이미지별 OCR 품질은 촬영 품질, 언어 데이터 다운로드, 브라우저 성능에 의존한다.
- Service worker 캐시 정책은 기본 앱 shell 중심이며, 로그인 세션이 필요한 API 응답은 캐시하지 않는다.
