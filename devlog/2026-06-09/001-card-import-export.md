# 명함 import/export 기능 추가

- **ID**: 001
- **날짜**: 2026-06-09
- **유형**: 기능 추가

## 작업 요약
명함 관리 화면에 CSV/TXT/XLSX 가져오기와 CSV/XLSX 내보내기를 추가했다.
가져오기는 파일 미리보기, 첫 행 머리글 설정, 구분 기호 선택, 컬럼 매핑, 중복 처리 정책을 거쳐 저장하도록 구성했다.

## 원문 요청사항
```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

작업 시작

## 리뷰 요약

- 리뷰 ID: hhphmfptentbkbpkcfuvjraaizlmbuij
- 제목: import/export 기능 추가

## 리뷰어 요청 내용

import, export 기능을 추가해줘.
import 기능은 csv, txt, xlsx 파일만 허용하고, 엑셀에 있는 기능과 거의 비슷한 느낌으로 하면 될 것 같아.
```

## 변경 파일 목록
- `src/app/page.cards/api.py`: import 미리보기/확정 저장/export API 및 CSV/TXT/XLSX 파서, CSV/XLSX 생성 로직 추가
- `src/model/struct/business_card.py`: export_rows, import_rows, 중복 감지 및 import update 시 기존 이미지 보존 로직 추가
- `src/app/page.cards/view.ts`: 파일 선택, 미리보기 호출, 컬럼 매핑, 중복 정책, 다운로드 처리 로직 추가
- `src/app/page.cards/view.pug`: 상단 import/export 버튼, import 설정/매핑/미리보기 모달 추가
- `src/app/page.cards/view.scss`: import 모달, 미리보기 테이블, 모바일 툴바 반응형 스타일 추가
- `README.md`: 명함 import/export API 목록 추가
- `tests/import_export_smoke.py`: import/export helper smoke 테스트 추가
- `devlog.md`, `devlog/2026-06-09/001-card-import-export.md`: 작업 이력 추가

## 검증 결과
- `python -m py_compile src/app/page.cards/api.py src/model/struct/business_card.py tests/import_export_smoke.py` 통과
- `python tests/import_export_smoke.py` 통과
- `wiz_project_build(clean=true)` 통과
- `npm run test:e2e` 통과: Chromium/모바일 Chromium 6개 smoke 테스트 통과

## 남은 리스크
- 실제 로그인 세션으로 대량 파일을 업로드해 DB에 반영하는 운영 데이터 검증은 수행하지 않았다.
- import 파일은 6MB, 최대 5,000행으로 제한했다.
