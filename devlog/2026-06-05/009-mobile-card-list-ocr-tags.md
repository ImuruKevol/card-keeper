# 모바일 명함 목록 및 OCR 태그 제거 UI 개선

- **ID**: 009
- **날짜**: 2026-06-05
- **유형**: UX 개선 / 기능 개선

## 작업 요약
모바일 `/cards` 화면 상단에서 제목, 검색, 중복 등록 패널을 숨기고 `명함 등록` 단일 버튼만 표시하도록 정리했다.
명함 목록은 첨부 예시처럼 텍스트 정보와 앞면 썸네일을 나란히 보여주는 리스트형 UI로 개선하고, OCR 태그 생성·노출 경로를 제거했다.
명함 앞면/뒷면 이미지를 저장·조회할 수 있도록 명함 모델과 저장 API에 이미지 필드를 연결했다.

## 원문 요청사항
```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

작업 시작

## 리뷰 요약

- 리뷰 ID: oepagepaprvegozicgwjffdstglpiryj
- 제목: UI 개선
- 요청 링크: https://bus.sub.nanoha.kr/
- Codex 요청자: 권태욱
- 프로젝트 루트: /opt/app
- Codex 세션 ID: 신규
- 스크린샷 컨텍스트: 없음
- 에이전트 작업 지시서 컨텍스트: 포함됨
- HTML 문서 생성 규칙 컨텍스트: 없음
- HTML 문서 설정 컨텍스트: 없음
- HTML 프로젝트 인스트럭션 파일: 없음
- 첨부파일 컨텍스트: 1개

## 에이전트 작업 지시서

# 에이전트 작업 지시서

## 리뷰 정보

- 리뷰 ID: oepagepaprvegozicgwjffdstglpiryj
- 제목: UI 개선
- 상태: open
- 우선순위: normal
- 분류: ux
- 프로젝트: 명함 관리 서비스
- 프로젝트 종류: web_service
- 요청 링크: https://bus.sub.nanoha.kr/
- 화면: 1440x900
- 캡처 방식: capture-unavailable-cross-origin
- 스크린샷 첨부: no
- 리뷰 첨부 파일: 1개

## 리뷰어 요청 내용

- 앱에서 볼 때 맨 위 상단에 명함 관리 및 검색, 사진 등록 부분이 있고, 그 아래에도 사진 기반 등록 카드가 있어서 중복된 정보가 화면을 꽉채우고 있어. 모바일 화면에서는 맨 위에 "명함 등록" 버튼 하나만 표시하면 되고, 나머지 불필요한 정보는 표시하지 말도록 해줘.
- OCR 시 태그 정보는 필요 없으니 삭제해줘.
- 각 명함별로 정보가 쭉 표시되는데, 목록 레이아웃은 첨부한 스크린샷을 참고해줘. 반드시 명함 앞면 이미지도 표시해야해. 디자인은 물론 훨씬 예쁘고 깔끔하게 개선해야해.

## 첨부 파일

- 1. KakaoTalk_20260605_172655136.png (image/png)

## 첨부파일 컨텍스트

선택한 첨부파일은 Codex 작업 서버의 아래 경로에서 사용할 수 있습니다.

- KakaoTalk_20260605_172655136.png (리뷰 첨부, image/png, 429.5KB): `/tmp/reviewops-codex-274beb5a7dc340b28e4bbb30b2dc8d99/attachments/01-KakaoTalk_20260605_172655136.png`
```

## 변경 파일 목록

### UI
- `src/app/page.cards/view.pug`: 모바일 목록을 앞면 썸네일 포함 리스트로 재구성하고, 태그 컬럼/입력 제거 및 등록 버튼 라벨 변경
- `src/app/page.cards/view.scss`: 모바일 상단 단일 CTA, 중복 등록 패널 숨김, 데스크톱/모바일 썸네일 목록 스타일 추가
- `src/app/page.cards/view.ts`: 앞면/뒷면 이미지 저장 payload 연결, 편집 시 저장 이미지 복원, OCR 태그 병합 제거

### API / 모델
- `src/app/page.cards/api.py`: 저장 payload에 `front_image`, `back_image` 포함, OCR 태그 생성 제거
- `src/model/db/business_card.py`: `front_image`, `back_image` LONGTEXT 필드 추가
- `src/model/struct/business_card.py`: 이미지 필드 정리/저장 및 기존 테이블 누락 컬럼 자동 보강, 연락처 검색에 전화번호 포함
- `src/model/struct/ai_setting.py`: AI OCR 스키마/프롬프트/정규화에서 tags 필드 제거

## 검증 결과
- `wiz_project_build(clean=false)`: 성공
- 개발 쿠키 `season-wiz-project=main`, `season-wiz-devmode=true` 포함 `curl -I https://bus.sub.nanoha.kr/cards`: HTTP 200
- Playwright 로그인 세션 + 개발 쿠키 검증:
  - 모바일 390x844: `/cards` 유지, 상단 `명함 등록` 버튼만 노출, 제목/검색/검색 버튼/사진 기반 등록 패널 숨김, 모바일 리스트와 썸네일 슬롯 렌더링
  - 데스크톱 1440x900: 테이블 목록과 앞면 썸네일 컬럼 렌더링, 모바일 리스트 숨김
- DB 확인: `business_card.front_image`, `business_card.back_image` 컬럼이 `longtext`로 존재
- 태그 노출/자동 생성 참조 검색: `tagList`, `태그`, `fields.tags`, `사진OCR`, `서버OCR`, `AIOCR` 잔여 참조 없음

## 남은 리스크
- 기존에 저장된 명함은 앞면 이미지가 없으면 `앞면 없음` placeholder가 표시된다. 새로 등록하거나 이미지를 다시 저장한 명함부터 실제 앞면 썸네일이 표시된다.
- 명함 이미지는 DB LONGTEXT에 저장하므로 등록량이 커지면 DB 용량 증가가 있을 수 있다. 현재 클라이언트 cropper는 이미지당 약 2.8MB 이하로 압축한다.
