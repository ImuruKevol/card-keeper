# 모바일 명함 상세 모달 편집·닫기 버튼 위치 고정

- **ID**: 002
- **날짜**: 2026-06-08
- **리뷰 ID**: fweonjkzhhwdiqjcrrjjkslndxppifyk
- **유형**: 버그 수정

## 작업 요약

모바일 상세 모달에서 헤더가 세로 컬럼으로 바뀌며 편집 버튼과 X 버튼이 본문 흐름 안으로 밀려 보이는 문제를 수정했다.
`detail-header-actions`를 모바일 구간에서 헤더 오른쪽 위 절대 위치로 고정하고, 편집 버튼이 전체 폭으로 늘어나지 않도록 상세 헤더 범위에서만 재정의했다.

## 원문 요청사항

```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

작업 시작

## 리뷰 요약

- 리뷰 ID: fweonjkzhhwdiqjcrrjjkslndxppifyk
- 제목: 명함 상세 모달 X 버튼 위치 수정
- 요청 링크: https://bus.sub.nanoha.kr/access
- Codex 요청자: 권태욱
- 프로젝트 루트: /opt/app
- Codex 세션 ID: 신규
- 스크린샷 컨텍스트: 없음
- 에이전트 작업 지시서 컨텍스트: 포함됨
- HTML 문서 생성 규칙 컨텍스트: 없음
- HTML 문서 설정 컨텍스트: 없음
- HTML 프로젝트 인스트럭션 파일: 없음
- 첨부파일 컨텍스트: 0개

## 리뷰어 요청 내용

PC 버전에서는 티가 별로 안나는데, 모바일에서는 편집 버튼과 X 버튼 위치가 이상한 위치에 들어가있음. 오른쪽 위로 고정이 되어야 함.
```

## 변경 파일 목록

- `src/app/page.cards/view.scss`
  - 모바일 `detail-header`를 상대 위치 기준점으로 설정
  - 모바일 `detail-header-actions`를 오른쪽 위 절대 위치의 flex 액션 그룹으로 재정의
  - 상세 헤더 안의 편집 버튼이 모바일 공통 `width: 100%` 규칙을 받지 않도록 폭/높이 재정의
- `devlog.md`
- `devlog/2026-06-08/002-mobile-detail-modal-actions.md`

## 검증 결과

- `wiz_project_build(clean=false, projectName="main")`: 성공
- Playwright 모바일 검증: 390x844 viewport, 쿠키 `season-wiz-project=main`, `season-wiz-devmode=true` 적용 후 `/cards`에서 실제 명함 상세 모달 열기 성공
- Playwright 레이아웃 측정: `.detail-header-actions`가 `position: absolute`, `display: flex`, 헤더 오른쪽 14px/위쪽 14px 위치에 배치됨. X 버튼 크기 38x38, 편집 버튼 폭 69px 확인

## 남은 리스크

- 운영 도메인에서는 cross-origin 캡처 제한이 있어 직접 스크린샷 비교는 하지 못했다. 로컬 WIZ 서버의 동일 프로젝트/실데이터 기반 모바일 레이아웃으로 검증했다.
