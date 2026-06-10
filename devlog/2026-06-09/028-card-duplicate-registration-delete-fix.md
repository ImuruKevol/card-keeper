# 명함 등록 중복 확인 모달 및 삭제 확인 버튼 수정

- **ID**: 028
- **날짜**: 2026-06-09
- **유형**: 버그 수정

## 작업 요약
명함 신규 저장 시 같은 이름의 활성 명함이 있으면 즉시 추가하지 않고 중복 후보를 반환하도록 API를 보강했다.
프론트 저장 흐름은 중복 응답을 받으면 덮어쓰기 또는 새로 추가를 선택하는 모달을 띄우고, 명함 삭제 확인 모달의 버튼 인자 순서를 수정했다.

## 원문 요청사항
```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

작업 시작해줘.
그리고 번외로 명함 삭제도 동작하지 않는 버그가 있어.

## 리뷰 요약

- 리뷰 ID: bicuxnguegisnjurlimfmmpfgphkmcii
- 제목: 명함 등록 시 버그
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

- 리뷰 ID: bicuxnguegisnjurlimfmmpfgphkmcii
- 제목: 명함 등록 시 버그
- 상태: in_progress
- 우선순위: normal
- 분류: bug
- 프로젝트: 명함 관리 서비스
- 프로젝트 종류: web_service
- 요청 링크: https://bus.sub.nanoha.kr/
- 화면: 1440x900
- 캡처 방식: browser-display-capture-element
- 스크린샷 첨부: yes
- 리뷰 첨부 파일: 0개

## 리뷰어 요청 내용

명함을 촬영하거나 해서 등록할 때 같은 이름이 있으면 같은사람인지 물어보면서 덮어씌울지 새로 추가할지 물어보는 모달이 추가되어야 해.
현재는 그냥 추가가 되고 있어.
```

## 변경 파일 목록
- `src/model/struct/business_card.py`
  - 같은 이름의 활성 명함 후보를 owner 범위에서 조회하는 `same_name_candidates()` 추가
  - 이름 비교 시 공백과 대소문자를 정규화하도록 보조 메서드 추가
- `src/app/page.cards/api.py`
  - 신규 저장 요청에서 같은 이름 후보가 있으면 `409`와 후보 요약을 반환
  - `duplicate_action=create` 재시도는 새 명함 추가로 허용
- `src/app/page.cards/view.ts`
  - `409` 중복 응답 시 덮어쓰기/새로 추가 선택 모달 표시
  - 덮어쓰기는 후보 ID로 저장 재시도, 새로 추가는 중복 허용 플래그로 저장 재시도
  - 명함 삭제 확인 모달의 action/cancel 버튼 인자 순서 수정
- `devlog.md`
  - 이번 작업 요약 행 추가
- `devlog/2026-06-09/028-card-duplicate-registration-delete-fix.md`
  - 이번 작업 상세 기록 추가

## 확인 결과
- `wiz_project_build(clean=false)` 성공
- 개발 쿠키 `season-wiz-project=main`, `season-wiz-devmode=true`와 유효 세션으로 `page.cards/list` API 인증 및 호출 확인
- 기존 이름으로 신규 저장 요청 시 쓰기 전에 `code=409`와 중복 후보가 반환되는 것을 확인
- 임시 테스트 명함 2건으로 중복 감지, `duplicate_action=create` 새로 추가, `remove` 삭제 API를 확인
- 테스트 명함 삭제 후 `Codex검증` 검색 결과 활성 항목 0건 확인

## 남은 리스크
- 실제 브라우저에서 촬영 UI를 통한 모달 표시와 버튼 시각 상태까지는 별도 스크린샷으로 검증하지 않았다.
- 같은 이름 후보가 여러 건이면 최근 수정된 첫 번째 후보에 덮어쓴다.
