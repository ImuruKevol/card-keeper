# PWA 명함 목록 여백 제거 및 검색/정렬 편의 기능 추가

- **ID**: 011
- **날짜**: 2026-06-05
- **유형**: UX 개선 / 기능 개선

## 작업 요약
PWA 모바일 명함 목록의 바깥 좌우 여백을 제거해 목록 배경이 화면 가장자리까지 이어지도록 조정했다.
명함 목록에 공통 필터 바를 추가해 데스크톱과 PWA 모두에서 전체/이름/정보 검색 범위와 최신순/이름순/회사별 정렬을 사용할 수 있게 했다.
목록 API와 명함 모델 조회 로직도 검색 범위와 정렬 파라미터를 처리하도록 연결했다.

## 원문 요청사항
```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

작업 시작

## 리뷰 요약

- 리뷰 ID: kivsqhpmuzfldxjyjomqgzrxcrwfklmr
- 제목: UI 추가 개선
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

- pwa 앱에서 보면 명함 목록에서 왼쪽 오른쪽에 여백이 보여서 뭔가 불편함.
- 이름순 정렬, 회사별 정렬, 이름/정보 등으로 검색 등 편의 기능 추가 필요.
```

## 변경 파일 목록

### UI
- `src/app/page.cards/view.pug`: 목록 필터 바 추가, 검색 범위 선택, 정렬 선택, 검색/초기화 버튼 추가
- `src/app/page.cards/view.scss`: 모바일 목록 바깥 여백 제거, 필터 바/셀렉트/모바일 목록 내부 여백 스타일 추가
- `src/app/page.cards/view.ts`: 검색 상태에 `scope`, `sort` 추가, placeholder/필터 초기화 헬퍼 추가

### API / 모델
- `src/app/page.cards/api.py`: 목록 API에서 `scope`, `sort` 파라미터 전달
- `src/model/struct/business_card.py`: 전체/이름/정보 검색 범위와 최신순/이름순/회사별 정렬 처리

### Devlog
- `devlog.md`
- `devlog/2026-06-05/011-pwa-list-search-sort.md`

## 검증 결과
- `wiz_project_build(clean=false, projectName="main")`: 성공
- `npm run test:e2e -- --project=chromium --project=mobile-chromium`: 성공, 개발 쿠키 `season-wiz-project=main`, `season-wiz-devmode=true` 적용된 smoke test 6개 통과
- `python -m py_compile src/app/page.cards/api.py src/model/struct/business_card.py`: 성공
- `PYTHONDONTWRITEBYTECODE=1 python tests/ocr_business_card_smoke.py`: 실패, 현재 테스트가 `tags: 양면` 병합 값을 기대하지만 현 API의 OCR 병합 필드에는 `tags`가 없어 발생

## 남은 리스크
- 실제 로그인 계정 비밀번호가 없어 `/cards` 로그인 후 목록 화면을 브라우저에서 직접 조작 검증하지 못했다.
- `tests/ocr_business_card_smoke.py`의 `tags` 기대값은 현재 API 동작과 맞지 않아 별도 정리가 필요하다.
