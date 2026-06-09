# 명함 목록 컬럼 정리 및 상세 연락 액션 추가

- **ID**: 011
- **날짜**: 2026-06-08
- **리뷰 ID**: kivsqhpmuzfldxjyjomqgzrxcrwfklmr
- **유형**: UX 개선 / 기능 개선

## 작업 요약

명함 목록 검색 아이콘 버튼을 제거하고, 기존 input Enter 검색 흐름만 남겼다.
PC 명함 목록에서 출처 컬럼을 제거하고 빈 상태 colspan을 맞췄다.
PC 상세 모달의 내부 스크롤을 줄이기 위해 상세 본문 간격과 패딩을 줄이고, 부서/직책을 한 줄 필드로 통합했다.
상세 모달의 이메일, 휴대폰, 전화, 웹사이트 필드에 직접 실행 버튼을 추가했다.

## 원문 요청사항

```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

- 검색 아이콘 버튼은 그냥 삭제하고, input에서 enter를 누르면 검색이 되도록 해줘.
- PC 버전의 명함 목록에서 출처 컬럼은 삭제해줘.
- PC 버전의 명함 상세 모달에서 스크롤이 생기지 않도록 해줘. 부서, 직책같은건 한줄로 통합할 수 있고, PC와 모바일 모두 각 정보간 간격이 너무 넓어.
- 웹사이트는 누르면 새 창에서 해당 웹사이트를 열 수 있는 버튼을 추가하고, 이메일은 mailto 링크를 추가해서 누르면 메일을 보낼 수 있는 버튼을 추가해줘. 휴대폰과 전화도 각각 모바일에서 전화를 바로 걸 수 있도록 링크를 추가해줘.

## 리뷰 요약

- 리뷰 ID: kivsqhpmuzfldxjyjomqgzrxcrwfklmr
- 제목: UI 추가 개선
- 요청 링크: https://bus.sub.nanoha.kr/
- Codex 요청자: 권태욱
- 프로젝트 루트: /opt/app
```

## 변경 파일 목록

- `src/app/page.cards/view.pug`
  - 검색 아이콘 버튼 제거
  - PC 목록 출처 컬럼 제거 및 colspan 수정
  - 상세 필드 행에 `메일`, `전화`, `열기` 링크 버튼 추가
- `src/app/page.cards/view.ts`
  - 부서/직책 상세 필드를 `부서/직책` 한 줄로 통합
  - `mailto:`, `tel:`, 웹사이트 새 창 링크 생성 로직 추가
- `src/app/page.cards/view.scss`
  - 상세 모달 폭/패딩/간격/필드 행/버튼 크기 조정
  - PC 상세 본문 overflow 숨김 처리
  - 모바일 상세 본문은 필요 시 스크롤 가능하도록 유지
  - 링크 버튼 스타일 추가
- `devlog.md`
- `devlog/2026-06-08/011-card-detail-contact-actions.md`

## 검증 결과

- `wiz_project_build(clean=false, projectName="main")`: 성공
- `npm run test:e2e -- --project=chromium --project=mobile-chromium`: 성공, 개발 쿠키 `season-wiz-project=main`, `season-wiz-devmode=true` 적용된 smoke test 6개 통과

## 남은 리스크

- 실제 로그인 계정 비밀번호가 없어 `/cards` 로그인 후 상세 모달의 새 링크 버튼과 스크롤 상태를 브라우저에서 직접 조작 검증하지 못했다.
- 상세 메모/주소가 매우 긴 데이터는 PC 모달에서 일부 영역이 다시 길어질 수 있다.
