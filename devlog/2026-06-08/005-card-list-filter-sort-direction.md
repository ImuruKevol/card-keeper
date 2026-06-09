# 명함 목록 검색 토글 및 정렬 방향 옵션 추가

- **ID**: 005
- **날짜**: 2026-06-08
- **리뷰 ID**: kivsqhpmuzfldxjyjomqgzrxcrwfklmr
- **유형**: UX 개선 / 기능 개선

## 작업 요약

명함 목록 검색 범위를 기존 `전체/이름/정보` select에서 `이름/회사` 토글 버튼으로 변경했다.
기본 검색 범위는 `이름`으로 설정하고, 회사 검색은 회사명과 부서명을 대상으로 조회하도록 했다.
정렬 기준은 `최신순/이름순/회사순`으로 유지하되, 별도 `오름차순/내림차순` 토글을 추가해 모든 정렬 기준에 방향을 적용했다.
PC 테이블 목록에서는 명함 썸네일 높이 때문에 텍스트와 작업 버튼이 위로 붙지 않도록 테이블 셀을 세로 가운데 정렬로 조정했다.

## 원문 요청사항

```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

- 전체 검색/이름 검색/정보 검색은 기본값을 이름 검색으로 하고, 이름/회사 검색 토글 버튼 형식으로 수정해줘.
- 최신순, 이름순, 회사순 정렬에 오름차순, 내림차순 정렬도 필요해.
- PC에서 볼 때 명함 목록에서 명함 카드 height때문에 다른 값들이 세로 가운데 정렬이 되지 않고 있어.

## 리뷰 요약

- 리뷰 ID: kivsqhpmuzfldxjyjomqgzrxcrwfklmr
- 제목: UI 추가 개선
- 요청 링크: https://bus.sub.nanoha.kr/
- Codex 요청자: 권태욱
- 프로젝트 루트: /opt/app
```

## 변경 파일 목록

- `src/app/page.cards/view.ts`
  - 검색 기본값을 `scope: name`, `direction: desc`로 변경
  - 이름/회사 검색 토글 및 정렬 방향 변경 헬퍼 추가
  - 필터 초기화 기준을 새 기본값에 맞춤
- `src/app/page.cards/view.pug`
  - 검색 범위 select를 `이름/회사` segmented control로 교체
  - `오름차순/내림차순` segmented control 추가
  - 회사 정렬 라벨을 `회사순`으로 정리
- `src/app/page.cards/view.scss`
  - segmented control 스타일 추가
  - 모바일 필터 바에서 토글이 줄바꿈되도록 반응형 처리
  - PC 테이블 셀 `vertical-align: middle` 적용
- `src/app/page.cards/api.py`
  - 목록 API 기본 검색 범위를 `name`으로 변경
  - `direction` 파라미터를 모델 조회로 전달
- `src/model/struct/business_card.py`
  - `company` 검색 범위 추가
  - 정렬 기준과 정렬 방향을 분리해 처리
- `devlog.md`
- `devlog/2026-06-08/005-card-list-filter-sort-direction.md`

## 검증 결과

- `wiz_project_build(clean=false, projectName="main")`: 성공
- `python -m py_compile src/app/page.cards/api.py src/model/struct/business_card.py`: 성공
- `npm run test:e2e -- --project=chromium --project=mobile-chromium`: 성공, 개발 쿠키 `season-wiz-project=main`, `season-wiz-devmode=true` 적용된 smoke test 6개 통과

## 남은 리스크

- 실제 로그인 계정 비밀번호가 없어 `/cards` 로그인 후 명함 목록 필터/정렬을 브라우저에서 직접 조작 검증하지 못했다.
- 회사 검색은 회사명과 부서명을 대상으로 하며, 직책/연락처/메모까지 포함하는 이전 `정보 검색` 범위는 UI에서 제거했다.
