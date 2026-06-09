# 명함 목록 카운트 위치 및 빠른 등록 버튼 개선

- **ID**: 017
- **날짜**: 2026-06-08
- **유형**: 버그 수정

## 작업 요약
PC 명함 목록 화면에서 빠른 등록 패널 오른쪽에 있던 총 명함 카운트를 검색 입력 바로 오른쪽으로 이동했다.
빠른 등록의 업로드/촬영 버튼은 데스크톱에서 큰 타일형 버튼으로 키워 오른쪽 영역의 빈 공간이 어색하게 남지 않도록 정리했다.

## 원문 요청사항
```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

작업 시작

## 리뷰 요약

- 리뷰 ID: vapgmcmvvuhubltgzkrchxynmmutounq
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

- 리뷰 ID: vapgmcmvvuhubltgzkrchxynmmutounq
- 제목: UI 수정
- 상태: open
- 우선순위: normal
- 분류: ux
- 프로젝트: 명함 관리 서비스
- 프로젝트 종류: web_service
- 요청 링크: https://bus.sub.nanoha.kr/
- 화면: 1440x900
- 캡처 방식: capture-unavailable-cross-origin
- 스크린샷 첨부: yes
- 리뷰 첨부 파일: 0개

## 리뷰어 요청 내용

- pc에서 빠른 등록 오른쪽의 총 명함 카운트를 검색 input 바로 오른쪽으로 옮겨줘. 그리고 업로드/촬영 버튼을 좀 많이 키워서 공간이 많이 남지 않고 어색하지 않도록 예쁘게 수정해줘.
```

## 변경 파일 목록
- `src/app/page.cards/view.pug`: 빠른 등록 패널의 `metric-strip` 카운트 블록을 제거하고, 목록 필터 행에서 검색 입력 바로 뒤에 `list-total-count`를 배치했다.
- `src/app/page.cards/view.scss`: 빠른 등록 패널을 2열 구조로 조정하고 업로드/촬영 버튼을 큰 타일형 버튼으로 키웠으며, 검색 입력 옆 총 명함 카운트 스타일을 추가했다.
- `devlog.md`: 본 작업 요약 행을 추가했다.
- `devlog/2026-06-08/017-card-count-search-and-capture-actions.md`: 작업 상세 기록을 추가했다.

## 검증 결과
- `wiz_project_build(clean=false, projectName="main")`: 성공.
- Playwright 직접 확인: `https://bus.sub.nanoha.kr/cards`에 `season-wiz-project=main`, `season-wiz-devmode=true`, 기존 활성 세션 쿠키를 적용해 1440x900 화면에서 확인.
  - 빠른 등록 패널의 기존 오른쪽 카운트 블록은 제거됨.
  - 총 명함 카운트가 검색 입력 바로 오른쪽에 표시됨.
  - 업로드/촬영 버튼은 각각 약 261x89px로 확대 표시됨.
  - 가로 오버플로우 없음.
- `npm run test:e2e`: 6개 테스트 모두 통과.

## 남은 리스크
- 검증은 기존 활성 관리자 세션 토큰을 사용했으며, 별도의 신규 로그인 비밀번호 기반 검증은 수행하지 않았다.
