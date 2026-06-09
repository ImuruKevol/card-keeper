# Android 전화 수신 발신자 표시 설계 문서 추가

- **ID**: 012
- **날짜**: 2026-06-09
- **유형**: 문서 업데이트

## 작업 요약

PWA 기반 명함장 서비스를 Android Galaxy 기기에서 전화 수신 시 발신자 정보 오버레이를 표시하는 방향으로 확장하기 위한 설계 문서를 추가했다.
Android `CallScreeningService`, 오버레이 권한, WIZ 명함 데이터 동기화, 스팸 번호 감지 API 조사 결과와 구현 단계/리스크를 정리했다.

## 원문 요청사항

```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

작업 시작

## 리뷰 요약

- 리뷰 ID: fsumlylodsndjbhmyxpxsxyukprfwbzd
- 제목: 안드로이드 앱 개발 설계
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

- 리뷰 ID: fsumlylodsndjbhmyxpxsxyukprfwbzd
- 제목: 안드로이드 앱 개발 설계
- 상태: open
- 우선순위: high
- 분류: design
- 프로젝트: 명함 관리 서비스
- 프로젝트 종류: web_service
- 요청 링크: https://bus.sub.nanoha.kr/
- 화면: 1440x900
- 캡처 방식: capture-unavailable-cross-origin
- 스크린샷 첨부: yes
- 리뷰 첨부 파일: 0개

## 리뷰어 요청 내용

현재 이 서비스는 pwa 앱 형태로 동작하고 있음.
근데 이걸 안드로이드(주로 갤럭시)에서 전화가 왔을 때 후스콜같은 앱처럼 전화가 왔을 때 해당 전화번호가 등록이 되어있으면 오버레이같은걸 띄워서 보여주고 싶어. 어떻게 하면 될지 방향성과 방법 등을 설계해서 문서 형태로 작성해줘.
이 때 후스콜 앱이나 에이닷같은 곳에서 제공하는 스팸 번호 감지같은 기능을 넣을 수 있으면 좋은데 혹시 공공 API같은게 있는지도 확인해줘.

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

- `docs/android-caller-id-design.md`: Android 전화 수신 발신자 표시 기능 설계 문서 신규 추가
- `devlog.md`: 이번 작업 요약 행 추가
- `devlog/2026-06-09/012-android-caller-id-design.md`: 이번 작업 상세 기록 추가

## 검증 결과

- `docs/android-caller-id-design.md` 작성 후 WIZ MCP로 파일 내용을 확인했다.
- Android 공식 문서, Google Play 권한 정책, KISA 불법스팸대응센터, 공공데이터포털 KISA WHOIS OpenAPI, Whoscall Anti-scam Intelligence 자료를 확인해 문서 참고 자료에 반영했다.
- 문서 작업만 수행했으므로 앱 빌드와 브라우저 테스트는 실행하지 않았다.

## 남은 리스크

- 공개 공공 API 부재 판단은 2026-06-09 공개 문서/검색 기준이며, 제휴형 또는 비공개 API 존재 여부는 KISA/통신사/상용 공급자에 별도 문의가 필요하다.
- Android overlay와 caller ID 역할 동작은 실제 Galaxy 기기와 OS 버전별 실기 테스트가 필요하다.
