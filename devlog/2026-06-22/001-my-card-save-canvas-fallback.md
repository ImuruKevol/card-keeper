# 내 명함 저장 시 캔버스 참조 오류 수정

- **ID**: 001
- **날짜**: 2026-06-22
- **유형**: 버그 수정

## 작업 요약
내 명함 정보 입력 영역의 저장 버튼이 다른 `*ngIf` 템플릿 스코프에 있는 `previewCanvas`를 넘기면서 `undefined`가 전달되던 문제를 수정했다.
저장, 공유, 다운로드, 공개 링크 생성 시 캔버스 인자가 없어도 현재 미리보기 캔버스 또는 임시 캔버스로 명함 이미지를 렌더링하도록 보강했다.

## 원문 요청사항
```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

작업 시작

## 리뷰 요약

- 리뷰 ID: wggzubegdylxgtxckrzlkysrtmaumzcc
- 제목: 내 명함 정보 저장 안됨
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

- 리뷰 ID: wggzubegdylxgtxckrzlkysrtmaumzcc
- 제목: 내 명함 정보 저장 안됨
- 상태: open
- 우선순위: normal
- 분류: bug
- 프로젝트: 명함 관리 서비스
- 프로젝트 종류: web_service
- 요청 링크: https://bus.sub.nanoha.kr/
- 화면: 1440x900
- 캡처 방식: browser-display-capture-element
- 스크린샷 첨부: no
- 리뷰 첨부 파일: 0개

## 리뷰어 요청 내용

내 명함 정보 입력 후 저장하면 아래 에러가 뜸.

---

page.my_card.component.ts:408 Uncaught (in promise) TypeError: Cannot set properties of undefined (setting 'width')
    at _PageMy_cardComponent.renderCardImage (page.my_card.component.ts:408:16)
    at _PageMy_cardComponent.prepareCardImage (page.my_card.component.ts:402:21)
    at async _PageMy_cardComponent.save (page.my_card.component.ts:159:38)

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
- `src/app/page.my_card/view.pug`: 정보 입력 영역의 데스크톱/모바일 저장 버튼에서 스코프 밖 `previewCanvas` 인자 전달을 제거.
- `src/app/page.my_card/view.ts`: 명함 이미지 생성 관련 메서드의 캔버스 인자를 optional로 변경하고, 실제 미리보기 캔버스 또는 임시 캔버스를 찾는 fallback 추가.
- `devlog.md`: 작업 요약 행 추가.
- `devlog/2026-06-22/001-my-card-save-canvas-fallback.md`: 작업 상세 기록 추가.

## 확인 결과
- `wiz_project_build(clean=false)` 성공.
- 변경 diff와 `page.my_card` 수정 내용을 확인했다.
- 인증된 브라우저 세션이 없어 실제 저장 API end-to-end 실행은 별도로 수행하지 못했다.
