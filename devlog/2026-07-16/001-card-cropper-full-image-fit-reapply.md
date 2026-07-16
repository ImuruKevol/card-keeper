# pull 기준 명함 업로드 전체 이미지 맞춤 재적용

- **ID**: 001
- **날짜**: 2026-07-16
- **유형**: 버그 수정
- **리뷰 ID**: hptnuhjgpvukdkihwokaufukdlsjwskl

## 작업 요약

GitHub pull 이후의 최신 cropper 구현을 기준으로 명함 이미지 축소 허용과 `전체 사용` 기능을 다시 적용했다.
최신 코드에 추가된 프레임 캐시, 마스크 합성, Angular 외부 이벤트 처리와 정리 로직은 유지했다.

## 원문 요청사항

```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

git이 꼬여서 수정한 부분들을 날리고 github에서 pull을 땡겨왔어.
현재 기준으로 다시 적용해줘.

## 리뷰 요약

- 리뷰 ID: hptnuhjgpvukdkihwokaufukdlsjwskl
- 제목: 명함 업로드 시 개선
- 요청 링크: https://bus.sub.nanoha.kr/
- Codex 요청자: 권태욱
- 프로젝트 루트: /opt/app
- Codex 세션 ID: 019f69a0-7c1a-7512-9e95-a4dac073101e
- Codex 모델: 5.6 sol (gpt-5.6-sol)
- Codex 추론수준: max (max)
- 스크린샷 컨텍스트: 없음
- 에이전트 작업 지시서 컨텍스트: 포함됨
- HTML 문서 생성 규칙 컨텍스트: 없음
- HTML 문서 설정 컨텍스트: 없음
- HTML 프로젝트 인스트럭션 파일: 없음
- 첨부파일 컨텍스트: 0개

## 세션 처리

저장된 Codex 세션을 resume해 이전 대화 맥락을 우선 사용하세요. 이전 Codex 히스토리는 이 요청에 포함되지 않습니다.

## 에이전트 작업 지시서

# 에이전트 작업 지시서

## 리뷰 정보

- 리뷰 ID: hptnuhjgpvukdkihwokaufukdlsjwskl
- 제목: 명함 업로드 시 개선
- 상태: in_progress
- 우선순위: normal
- 분류: ux
- 프로젝트: 명함 관리 서비스
- 프로젝트 종류: web_service
- 요청 링크: https://bus.sub.nanoha.kr/
- 화면: 1440x900
- 캡처 방식: browser-display-capture-element
- 스크린샷 첨부: yes
- 리뷰 첨부 파일: 0개

## 리뷰어 요청 내용

명함 이미지를 업로드하거나 찍을 때 명함 영역 맞춤 기능이 있음.
근데 명함 이미지가 가득 차있으면 가득한 명함 이미지를 맞출 수 있는 방법이 없음.

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

- `src/app/page.cards/view.ts`
  - cropper 최소 배율의 1배 하한을 제거하고 프레임을 덮는 실제 최소 배율을 사용했다.
  - 전체 원본 이미지를 최대 2200px·기존 JPEG 품질 정책으로 저장하는 동작을 추가했다.
  - 잘라내기와 전체 이미지 적용의 저장·이벤트 정리 로직을 공통화했다.
- `src/app/page.cards/view.pug`
  - 명함 영역 맞춤 화면에 `전체 사용` 버튼과 접근성 설명을 추가했다.
- `src/app/page.cards/view.scss`
  - `전체 사용` 버튼을 시각적으로 구분하고 모바일 액션을 2열로 배치했다.
- `devlog.md`
  - 재적용 작업의 요약 행을 추가했다.
- `devlog/2026-07-16/001-card-cropper-full-image-fit-reapply.md`
  - 요청 원문, 변경 내용, 검증 결과를 기록했다.

## 검증 결과

- `wiz_project_build(clean=false, projectName="main")`: 성공
- `git diff --check -- src/app/page.cards/view.ts src/app/page.cards/view.pug src/app/page.cards/view.scss`: 통과
- `npm run test:e2e`: 성공, Chromium·Mobile Chromium 합계 6 passed

## 남은 리스크

- 테스트 계정이 없어 인증된 실제 모바일 PWA에서 촬영 파일을 선택한 뒤 드래그·축소·`전체 사용` 결과까지 직접 확인하지 못했다.
