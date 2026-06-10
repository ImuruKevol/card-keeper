# 모바일 상단 메뉴 관리자 항목 숨김 및 일반 메뉴 라벨 표시

- **ID**: 026
- **날짜**: 2026-06-09
- **유형**: UI 수정

## 작업 요약
모바일 상단 메뉴에서 관리자 전용 `사용자 관리`, `AI 설정` 항목을 숨기고, PC에서는 기존처럼 노출되도록 분리했다.
모바일에서 남는 공간을 활용해 `목록`, `내 명함` 일반 메뉴 라벨을 아이콘 옆에 표시하고 작은 화면 폭 보정을 유지했다.

## 원문 요청사항
```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

AI 설정, 사용자 관리 메뉴 버튼은 모바일 화면에서는 그냥 안보이게 해줘. PC에서만 보이도록 해줘.
그러면 명함 목록과 내 명함 보기 메뉴를 조금 더 크게 하거나 텍스트를 추가할 수 있을 것 같아.

## 리뷰 요약

- 리뷰 ID: feedyfzkuwqnwozrdtzgsqbqtjkeujma
- 제목: UI 수정
- 요청 링크: https://bus.sub.nanoha.kr/
- Codex 요청자: 권태욱
- 프로젝트 루트: /opt/app
- Codex 세션 ID: 019eab7c-7698-7af3-ab12-715de3988897
- 스크린샷 컨텍스트: 없음
- 에이전트 작업 지시서 컨텍스트: 없음
- HTML 문서 생성 규칙 컨텍스트: 없음
- HTML 문서 설정 컨텍스트: 없음
- HTML 프로젝트 인스트럭션 파일: 없음
- 첨부파일 컨텍스트: 0개

## 세션 처리

저장된 Codex 세션을 resume해 이전 대화 맥락을 우선 사용하세요. 이전 Codex 히스토리는 이 요청에 포함되지 않습니다.
```

## 변경 파일 목록
- `src/app/layout.sidebar/view.pug`: 일반 메뉴와 관리자 메뉴에 구분 class를 추가하고, 모바일 전용 라벨을 분리했다.
- `src/app/layout.sidebar/view.scss`: 모바일에서 관리자 메뉴를 숨기고 일반 메뉴 라벨을 표시하도록 반응형 스타일을 조정했다.
- `devlog.md`, `devlog/2026-06-09/026-mobile-topnav-primary-labels.md`: 작업 이력 기록.

## 검증 결과
- `wiz_project_build(projectName="main", clean=false)` 성공.
- `npm run test:e2e` 성공: Chromium/모바일 Chromium 총 6개 Playwright smoke 테스트 통과.
- Playwright smoke는 기존 설정의 `season-wiz-project=main`, `season-wiz-devmode=true` 쿠키 주입 상태로 실행했다.

## 남은 리스크
- 인증된 관리자 계정 정보가 없어 로그인 후 실제 관리자 세션에서 모바일 상단 메뉴가 숨겨지는지 수동 시각 검증은 수행하지 못했다.
