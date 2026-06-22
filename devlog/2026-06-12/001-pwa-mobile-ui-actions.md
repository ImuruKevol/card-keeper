# PWA 모바일 UI 개선

- **ID**: 001
- **날짜**: 2026-06-12
- **유형**: UX 개선

## 작업 요약
PWA 모바일 상단 메뉴가 화면 중앙에 배치되도록 헤더의 모바일 레이아웃을 3컬럼 그리드로 조정했다.
명함 관리 화면의 주요 액션 버튼 라벨을 요청 문구로 변경하고, import 아이콘은 업로드와 구분되도록 아래 방향 트레이 아이콘으로 교체했다.

## 원문 요청사항
```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

작업 시작

## 리뷰 요약

- 리뷰 ID: mafsqshttwkzwgjtxiwnbtbhylclhtsl
- 제목: PWA 모바일 UI 개선
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

## 리뷰어 요청 내용

PWA 앱을 모바일에서 열면 상단에 메뉴가 2개가 나옴. (목록, 내 명함)
근데 이게 왼쪽으로 쏠려있어서 보기가 불편함. 가운데정렬로 해줘.

---

버튼 이름 변경이 필요해.
가져오기 -> import
내보내기 -> CSV/XLSX
업로드 -> 이미지 업로드
촬영 -> 명함 촬영

그리고 현재 가져오기 버튼의 아이콘의 화살표 방향이 반대가 되어야 할 것 같아. 업로드와 비슷한 느낌이라서...
```

## 변경 파일 목록
- `src/app/layout.sidebar/view.scss`
  - 모바일/터치 헤더에서 브랜드, 메뉴, 액션 영역을 3컬럼 그리드로 배치
  - `.app-nav`를 중앙 컬럼에 `justify-self: center`로 고정하고 우측 액션은 끝 정렬로 조정
- `src/app/page.cards/view.pug`
  - 주요 버튼 라벨 변경: `import`, `CSV/XLSX`, `이미지 업로드`, `명함 촬영`
  - import 버튼 title/aria-label 갱신 및 아래 방향 트레이 아이콘으로 교체
  - 내보내기/import 모달의 실행 버튼 문구도 새 라벨 체계에 맞춰 조정
- `devlog.md`
  - 이번 작업 요약 행 추가
- `devlog/2026-06-12/001-pwa-mobile-ui-actions.md`
  - 이번 작업 상세 기록 추가

## 검증 결과
- `wiz_project_build(clean=false)` 성공.
- `git diff --check -- src/app/layout.sidebar/view.scss src/app/page.cards/view.pug` 통과.
- `curl -I -b "season-wiz-project=main; season-wiz-devmode=true" http://localhost:3000/cards` 200 OK 확인.
- 빌드 산출물 `main.js`에서 변경된 라벨과 import 아이콘 path 반영 확인.
- Playwright로 동일 헤더 마크업과 CSS를 390px 모바일 폭에서 측정해 `.app-nav` 중앙 오차 0px 확인.

## 비고
- 로컬 브라우저에서 실제 `/cards` 렌더링은 인증 세션이 없어 `/access`로 이동되어 직접 확인하지 못했다.
