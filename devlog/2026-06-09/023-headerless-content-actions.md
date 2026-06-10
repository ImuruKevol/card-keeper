# 주요 화면 헤더 제거 및 액션 컨트롤 콘텐츠 영역 재배치

- **ID**: 023
- **날짜**: 2026-06-09
- **유형**: UI 수정

## 작업 요약
명함 관리, 내 명함, 사용자 관리, AI 설정 화면의 페이지 헤더 블록을 제거하고 기존 헤더 기능을 각 화면의 콘텐츠 카드와 컨트롤 영역으로 옮겼다.
PC와 모바일 모두에서 버튼과 검색 컨트롤이 화면 폭에 맞게 배치되도록 반응형 스타일을 함께 조정했다.

## 원문 요청사항
```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

작업 시작

## 리뷰 요약

- 리뷰 ID: feedyfzkuwqnwozrdtzgsqbqtjkeujma
- 제목: UI 수정
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

- 리뷰 ID: feedyfzkuwqnwozrdtzgsqbqtjkeujma
- 제목: UI 수정
- 상태: open
- 우선순위: high
- 분류: ux
- 프로젝트: 명함 관리 서비스
- 프로젝트 종류: web_service
- 요청 링크: https://bus.sub.nanoha.kr/
- 화면: 1440x900
- 캡처 방식: browser-display-capture-element
- 스크린샷 첨부: no
- 리뷰 첨부 파일: 0개

## 리뷰어 요청 내용

- 각 화면별로 헤더가 있는데, 헤더를 제거하고, 기존 기능들은 컨텐츠 영역으로 옮기기
  - 명함 관리: 가져오기, 내보내기, 명함 등록 기능은 사진 기반 등록 텍스트 부분을 제거하고 그 위치로 옮기기
  - 내 명함: pc 기준 저장 버튼은 정보 입력 카드 헤더의 오른쪽 끝으로 이동
  - 사용자 관리: input, 검색 버튼은 카테고리 토글 버튼 오른쪽으로 옮기고 select 제거
  - AI 설정: 모델 불러오기는 API Key input 오른쪽으로 이동, 저장은 모델 id input 아래로 이동
모바일 버전에서도 알맞게 UI 수정할 것.

## 첨부 파일

-

## 콘솔 로그 요약

-

## 네트워크 로그 요약

-

## 환경 로그 요약

- browser-fingerprint: MacIntel / ko-KR / 2560x1440
- iframe-fingerprint: restricted / https://bus.sub.nanoha.kr
- iframe-fingerprint: restricted / https://bus.sub.nanoha.kr
- browser-fingerprint: MacIntel / ko-KR / 2560x1440
- iframe-fingerprint: restricted / https://bus.sub.nanoha.kr
- reviewops-sdk: SDK missing
- browser-fingerprint: MacIntel / ko-KR / 2560x1440
- iframe-fingerprint: restricted / https://bus.sub.nanoha.kr
```

## 변경 파일 목록
- `src/app/page.cards/view.pug`: 페이지 헤더 제거, 가져오기/내보내기/명함 등록 버튼을 빠른 등록 패널의 텍스트 영역으로 이동.
- `src/app/page.cards/view.scss`: 이동한 액션 버튼 그룹 스타일 추가, 모바일에서 빠른 등록 패널이 숨겨지지 않도록 조정.
- `src/app/page.my_card/view.pug`: 페이지 헤더 제거, 명함 관리 링크와 PC 저장 버튼을 정보 입력 카드 헤더 오른쪽 액션 영역으로 이동.
- `src/app/page.my_card/view.scss`: 정보 입력 카드 헤더 액션 영역과 모바일 저장/관리 링크 반응형 스타일 추가.
- `src/app/page.users/view.pug`: 페이지 헤더 제거, 상태 토글 오른쪽에 검색 input/검색 버튼 배치, 역할 필터 select 제거.
- `src/app/page.users/view.scss`: 사용자 관리 컨트롤 행 및 모바일 스택 레이아웃 추가.
- `src/app/page.ai_settings/view.pug`: 페이지 헤더 제거, 모델 불러오기 버튼을 API Key 입력 오른쪽으로 이동, 모델 ID 입력과 그 아래 저장 버튼 배치.
- `src/app/page.ai_settings/view.scss`: API Key inline 액션과 저장 버튼 영역 반응형 스타일 추가.
- `devlog.md`, `devlog/2026-06-09/023-headerless-content-actions.md`: 작업 이력 기록.

## 검증 결과
- `wiz_project_build(projectName="main", clean=false)` 성공.
- `npm run test:e2e` 성공: Chromium/모바일 Chromium 총 6개 Playwright smoke 테스트 통과.
- Playwright smoke는 `season-wiz-project=main`, `season-wiz-devmode=true` 쿠키를 주입한 기존 설정으로 실행했다.

## 남은 리스크
- 인증된 관리자 계정 정보가 없어 로그인 후 실제 데이터가 있는 내부 화면의 수동 시각 검증은 수행하지 못했다.
