# import 메모 병합 구분자 및 기본 옵션 조정

- **ID**: 004
- **날짜**: 2026-06-09
- **유형**: 기능 추가

## 작업 요약
매핑되지 않은 컬럼을 메모에 추가할 때 `key: value / key: value` 형태로 구분되도록 변경했다.
해당 체크박스는 파일을 선택한 직후 기본 활성화 상태가 되도록 조정했다.

## 원문 요청사항
```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

메모에 그냥 "key: value key: value" 형태로만 들어가는데, 쉼표나 슬래쉬같은걸로 해서 구분할 수 있도록 넣어줘.
그리고 매핑 안된 컬럼을 메모에 추가는 기본적으로 활성화 상태로 해줘.
```

## 변경 파일 목록
- `src/app/page.cards/api.py`: 매핑 제외 컬럼 메모 병합 구분자를 ` / `로 변경하고 API 기본값을 활성화로 조정
- `src/app/page.cards/view.ts`: import 옵션 기본값을 활성화로 변경하고 결과 미리보기 메모 구분자도 ` / `로 통일
- `tests/import_export_smoke.py`: 메모 병합 구분자 검증 케이스 보강
- `devlog.md`, `devlog/2026-06-09/004-import-memo-format-default.md`: 작업 이력 추가

## 검증 결과
- `python -m py_compile src/app/page.cards/api.py tests/import_export_smoke.py` 통과
- `python tests/import_export_smoke.py` 통과
- `wiz_project_build(clean=false)` 통과
- `npm run test:e2e` 통과: Chromium/모바일 Chromium 6개 smoke 테스트 통과

## 남은 리스크
- 실제 브라우저 import 화면에서 체크박스 기본 활성화 상태를 육안 검증하지는 못했다.
