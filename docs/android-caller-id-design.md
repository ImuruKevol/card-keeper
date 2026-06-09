# Android 전화 수신 명함 오버레이 설계

- 작성일: 2026-06-09
- 대상 서비스: 명함장 PWA (`https://bus.sub.nanoha.kr/`)
- 지원 범위: Galaxy / One UI 8.5 이상 전용
- 목표: Android 기기에서 전화가 왔을 때 등록된 명함 전화번호를 감지하고, 명함 이미지 중심의 위치 조정 가능한 오버레이를 표시한다.

## 결론

현재 PWA만으로는 전화 수신 이벤트, 발신 번호, 시스템 전화 화면 위 UI를 안정적으로 제어할 수 없다. Android 네이티브 동반 앱을 별도로 개발해야 한다. 기존 WIZ PWA는 명함 관리와 서버 API 역할을 유지하고, Android 앱은 로그인, 명함 동기화, 전화 수신 감지, 이미지 캐시, 오버레이 표시를 담당하는 구조가 적합하다.

권장 MVP는 다음 조합이다.

1. Android 앱에서 `CallScreeningService`를 구현하고 사용자가 이 앱을 발신자 표시/전화 선별 역할로 선택한다.
2. WIZ 서버에서 사용자의 활성 명함 전화번호와 오버레이 표시용 명함 이미지를 Android 앱으로 증분 동기화한다.
3. Android 앱은 Room 로컬 DB에 정규화된 전화번호 인덱스와 명함 이미지 캐시를 저장한다.
4. 전화가 오면 로컬 DB에서 즉시 조회하고, 매칭되면 오버레이 권한이 있는 경우 `TYPE_APPLICATION_OVERLAY` 창을 띄운다.
5. 오버레이 권한이 없거나 OS가 제한하면 heads-up 알림으로 fallback한다.
6. 오버레이 위치는 앱 설정에서 조정 가능하게 하고, 조정값은 기기 로컬 설정으로 저장한다.

## 지원 대상 고정

이번 설계는 Galaxy / One UI 8.5 이상만 지원한다. One UI 8.5 미만 기기와 비 Galaxy Android 기기는 지원 대상에서 제외한다.

지원 기준을 좁히는 이유:

- 전화 화면 위 오버레이 동작은 제조사 UI와 OS 정책 영향을 크게 받는다.
- 구버전별 예외 처리를 줄이고 One UI 8.5 이상 실제 기기에서만 QA한다.
- 사용자는 최신 Galaxy 환경 기준으로 권한 안내와 위치 조정 UX를 받는다.

검증 대상 예시:

- One UI 8.5 이상이 설치된 Galaxy S/Z/Fold/Flip 계열
- 잠금화면, 통화 수신 화면, 듀얼심, 절전 모드, 세로/가로 회전 상태
- 오버레이 권한 허용/거부 상태

Samsung은 2026-05-06부터 One UI 8.5 공식 rollout을 확대한다고 공지했고, 지역과 모델별 제공 시점은 다를 수 있다고 안내했다. 실제 지원 기기는 Samsung Members 또는 기기 설정의 소프트웨어 업데이트 상태를 기준으로 판정한다.

## Android 제약과 가능 범위

### PWA 한계

- PWA/브라우저 앱은 Android 전화 수신 이벤트를 받을 수 없다.
- Web Push나 Service Worker는 전화 앱 위에 실시간 명함 UI를 띄우는 용도로 사용할 수 없다.
- 따라서 홈 화면 설치형 PWA는 유지하되, 전화 수신 기능은 네이티브 앱 기능으로 분리해야 한다.

### 권장 Android API

`CallScreeningService`가 가장 맞는 진입점이다. Android 프레임워크는 사용자가 관련 역할로 선택한 앱의 `CallScreeningService`를 수신 전화가 울리기 전에 바인딩하고 `onScreenCall(Call.Details)`로 통화 정보를 전달한다. Android 10(API 29)부터는 `RoleManager.ROLE_CALL_SCREENING`을 통해 역할 요청 흐름을 구성할 수 있다.

대안은 기본 전화 앱(`ROLE_DIALER`)이 되는 방식이지만, 사용자 경험과 심사 부담이 커서 1차 목표에는 과하다. Accessibility Service로 전화 화면을 읽는 방식은 정책/보안 리스크가 크므로 사용하지 않는다.

## 권한 설계

| 항목 | 필요성 | 비고 |
|------|--------|------|
| `BIND_SCREENING_SERVICE` | 필수 | `CallScreeningService` 선언에 사용. 사용자가 발신자 표시/전화 선별 역할을 부여해야 동작한다. |
| `SYSTEM_ALERT_WINDOW` | 선택 | 전화 화면 위 명함 이미지 오버레이를 띄우는 권한. 사용자가 설정 화면에서 직접 허용해야 한다. |
| `POST_NOTIFICATIONS` | 선택 | Android 13+에서 오버레이 fallback 알림에 필요하다. |
| `INTERNET` | 필수 | WIZ 서버 동기화와 토큰 갱신에 필요하다. |
| `READ_CONTACTS` | 비권장 | 현재 서비스 명함 DB만 사용하면 필요 없다. |
| `READ_CALL_LOG` | 비권장 | 통화 기록 기능을 넣지 않으면 요청하지 않는다. |
| `READ_PHONE_STATE` | 가급적 회피 | `CallScreeningService`로 번호를 받는 구조에서는 핵심 경로에 두지 않는다. |

오버레이는 Android 8.0 이후 `TYPE_APPLICATION_OVERLAY`를 사용해야 하며, 시스템은 해당 창을 이동/크기 조정하거나 일부 중요 시스템 창보다 아래에 둘 수 있다. Android 10 이후 백그라운드 Activity 시작 제한도 있으므로, 전화 수신 시 Activity를 직접 띄우는 방식보다 서비스 기반 overlay 또는 알림 fallback이 안정적이다.

## 전체 아키텍처

```text
WIZ PWA / 서버
  - 명함 CRUD 유지
  - 모바일 동기화 API 추가
  - 모바일 기기 토큰/폐기 관리
  - 명함 이미지 정리/썸네일 API 추가
  - 이미지가 없는 명함의 생성 이미지 규격 제공

Android 앱
  - 로그인 또는 웹 세션 연동
  - WorkManager 증분 동기화
  - Room 로컬 번호 인덱스
  - 명함 이미지 로컬 캐시
  - CallScreeningService 수신 이벤트 처리
  - OverlayService / heads-up notification 표시
  - 오버레이 위치 조정 설정
```

## 전화 수신 처리 흐름

```text
1. 전화 수신
2. Android Telecom framework가 역할 앱의 CallScreeningService 호출
3. onScreenCall(Call.Details)에서 발신 번호 획득
4. 번호 정규화
5. Room DB의 phone_numbers 인덱스 조회
6. 매칭된 명함의 overlay_image 선택
7. 사용자 설정 위치에 오버레이 표시
8. 사용자가 통화 종료, 오버레이 닫기, 또는 일정 시간 경과 시 UI 제거
```

실시간 전화 수신 경로에서는 네트워크 요청에 의존하지 않는다. 전화 수신 시점에는 로컬 DB와 로컬 이미지 캐시만 사용하고, 서버 동기화는 백그라운드에서 미리 완료해 둔다.

## 명함 이미지 표시 정책

오버레이는 텍스트 카드가 아니라 명함 이미지 자체를 중심으로 표시한다. 이미지 선택 우선순위는 다음과 같다.

1. `business_card.front_image`가 있으면 앞면 이미지를 표시한다.
2. 앞면 이미지가 없고 `business_card.back_image`가 있으면 뒷면 이미지를 표시한다.
3. 저장된 명함 이미지가 없으면 명함 필드로 생성한 가상 명함 이미지를 표시한다.

가상 명함 이미지는 기존 `/my-card`의 명함 이미지 생성 방식과 같은 방향으로 만든다. 현재 `/my-card`는 1200x680 Canvas 렌더러를 사용하고, 저장/공유 이미지를 JPEG data URL로 만든다. Android 기능도 같은 시각 규칙을 따르되, 전화 수신 시 즉시 표시할 수 있도록 동기화 시점에 미리 생성/캐시한다.

### 가상 명함 이미지 생성 입력

- 이름
- 회사
- 부서 / 직책
- 휴대폰 / 대표전화
- 이메일
- 웹사이트
- 주소
- 태그 또는 메모 일부
- 기본 색상/강조 색상
- 테마: `signature`, `lattice`, `flow`

기본 색상과 테마는 다음 순서로 결정한다.

1. 명함별 표시 설정이 생기면 해당 설정 사용
2. 사용자의 `/my-card` 색상과 테마 설정 사용
3. 앱 기본값 사용: `#123c69`, `#14b8a6`, `signature`

### 이미지 캐시 전략

- 서버 sync는 이미지 원본 data URL을 매번 내려주지 않는다.
- `card_id`, `image_kind`, `image_hash`, `updated_at`을 먼저 동기화한다.
- Android 앱은 hash가 바뀐 명함만 이미지 다운로드/생성을 수행한다.
- 오버레이 표시용 이미지는 1200x680 기준 JPEG와 화면 표시용 thumbnail을 로컬 파일로 캐시한다.
- 이미지 캐시가 아직 없으면 즉시 Android 로컬 렌더러로 가상 명함 이미지를 만들고, 이후 서버 동기화 결과로 교체한다.

## 데이터 동기화 설계

현재 `business_card` 모델은 `mobile`, `phone`, `front_image`, `back_image` 필드를 가지고 있고, import 중복 판단에서 숫자만 추출한 정규화 로직을 사용한다. Android 기능을 위해서는 서버/클라이언트 양쪽에 같은 정규화 규칙과 이미지 캐시 규칙을 명시해야 한다.

### 번호 정규화 규칙

- 공백, 하이픈, 괄호, 점 제거
- `tel:` prefix 제거
- 내선 표기는 본번호와 분리 저장
- 한국 번호 기준 `+82`, `82` prefix를 국내형 `0` 시작 번호와 함께 alias로 저장
- `01012345678`, `821012345678`, `+821012345678`을 같은 번호로 매칭
- `mobile`, `phone` 각각 원본값과 정규화값을 별도 보관

### Android 로컬 DB 예시

```text
cards
- id
- name
- company
- department
- position
- mobile
- phone
- email
- website
- address
- tags
- memo_preview
- front_image_hash
- back_image_hash
- generated_image_hash
- overlay_image_kind: front | back | generated
- updated_at

phone_numbers
- normalized_number primary/index
- card_id
- kind: mobile | phone
- display_number
- updated_at

overlay_images
- card_id
- image_kind: front | back | generated
- image_hash
- local_path
- thumbnail_path
- width
- height
- updated_at

overlay_settings
- position_preset: top | middle | bottom | custom
- gravity: top_start | top_end | bottom_start | bottom_end | center
- offset_x_dp
- offset_y_dp
- scale
- opacity
- updated_at
```

### WIZ 서버 API 초안

새로운 Android 전용 route를 두는 편이 좋다. 기존 page API는 웹 화면 세션에 묶여 있으므로 네이티브 앱의 장기 토큰/기기 폐기/증분 동기화에는 별도 route가 더 명확하다.

```text
POST /api/mobile/auth/login
- 이메일/비밀번호 또는 웹 세션 교환
- 응답: access token, refresh token, device id

POST /api/mobile/auth/refresh
- refresh token으로 access token 재발급

POST /api/mobile/auth/logout
- 현재 device token 폐기

GET /api/mobile/cards/sync?since=2026-06-09T00:00:00Z
- 활성 명함의 표시 필드, 전화번호, 이미지 hash, updated/deleted marker 반환

GET /api/mobile/cards/{id}/overlay-image?kind=front|back|generated
- 오버레이 표시용 이미지 또는 생성 이미지 반환

POST /api/mobile/devices/{id}/overlay-settings
- 기기별 오버레이 위치/크기 설정 저장이 필요할 때 사용
```

MVP에서는 오버레이 위치 설정을 Android 로컬에만 저장해도 된다. 여러 기기 간 동일 위치를 유지해야 할 때만 서버 저장을 추가한다.

## 오버레이 UI 설계

오버레이는 명함 이미지가 주인공인 작은 floating panel로 둔다.

구성:

- 명함 이미지: 앞면/뒷면/생성 이미지 중 우선순위에 따른 이미지
- 하단 보조 줄: 이름, 회사, 전화번호를 작은 텍스트로 표시
- 액션: 닫기, 명함 상세 열기, 위치 조정
- 잠금화면 모드: 명함 이미지만 표시하거나, 보조 줄에서 메모/태그를 제외

표시 크기:

- 기본 너비: 화면 너비의 78% 이하
- 최대 너비: 420dp
- 최소 너비: 280dp
- 비율: 기존 명함 이미지 1200x680 비율 유지
- 사용자가 scale을 80~120% 범위에서 조정 가능

전화 수신/거절 버튼을 가리지 않는 것이 우선이다. One UI 8.5 실제 전화 화면 기준으로 기본 위치를 `bottom_end` 또는 `top_end` 중 더 안전한 위치로 잡고, 사용자가 직접 옮길 수 있게 한다.

## 오버레이 위치 조정

위치 조정은 앱 설정 화면과 오버레이 자체의 `위치 조정` 액션에서 모두 진입할 수 있게 한다.

설정 항목:

- 위치 프리셋: 상단, 중앙, 하단, 사용자 지정
- 기준점: 좌상단, 우상단, 좌하단, 우하단, 중앙
- X/Y offset: dp 단위 stepper 또는 drag로 조정
- 크기: 80~120% slider
- 투명도: 85~100% slider
- 테스트 오버레이 보기
- 기본값으로 복원

동작 규칙:

- 사용자가 오버레이를 drag하면 즉시 `custom` 위치로 전환한다.
- 세로/가로 방향별 위치를 따로 저장한다.
- 화면 밖으로 나가지 않도록 safe area와 최소 margin을 적용한다.
- 통화 수신 화면의 주요 버튼 영역과 겹치면 자동으로 위/아래로 밀어낸다.
- 위치 조정 중에는 전화 수신이 아니어도 테스트 이미지를 띄워 실제 표시 위치를 확인할 수 있게 한다.

Android 구현은 `WindowManager.LayoutParams.gravity`, `x`, `y`를 사용하고, 위치 값은 `DataStore`에 저장한다. 오버레이 권한이 꺼져 있으면 동일 설정 화면에서 알림 fallback 미리보기를 보여준다.

## 보안과 개인정보

- 전화번호 매칭은 사용자 본인의 명함 데이터만 로컬에 저장해 수행한다.
- Android 앱 토큰은 Android Keystore로 보호한다.
- 명함 이미지 캐시는 앱 전용 저장소에 두고 로그에 data URL을 남기지 않는다.
- 잠금화면에서는 메모, 태그, 상세 주소처럼 민감할 수 있는 텍스트를 숨길 수 있는 설정을 둔다.
- 로그에는 원본 전화번호 대신 hash 또는 마지막 4자리만 남긴다.
- 기기 분실/교체에 대비해 서버에서 모바일 기기 토큰 폐기 기능을 제공한다.

## 단계별 구현 계획

### 0단계: One UI 8.5 기술 검증

- 빈 Android 앱에 `CallScreeningService` 구현
- One UI 8.5 이상 Galaxy에서 역할 부여 흐름 확인
- 수신 번호 획득 가능성, 듀얼심, 잠금화면, 절전 상태 확인
- 전화 앱 위 `TYPE_APPLICATION_OVERLAY` z-order 확인
- 위치 조정 drag, safe area, 통화 버튼 회피 동작 확인

### 1단계: 서버 동기화 API

- 모바일 기기 토큰 테이블 추가
- 명함 증분 sync route 추가
- 서버/Android 공통 번호 정규화 테스트 케이스 정의
- `front_image`, `back_image`, 생성 이미지 hash 정책 정의
- 오버레이 이미지 다운로드 route 추가
- 삭제/비활성 명함 tombstone 처리

### 2단계: Android MVP

- Kotlin + Jetpack Compose + Room + WorkManager + Retrofit/Ktor 구성
- 로그인/토큰 저장(Android Keystore) 구현
- 명함 sync 및 로컬 번호 인덱스 구현
- 명함 이미지 캐시와 가상 명함 이미지 렌더러 구현
- `CallScreeningService`와 overlay/notification 구현
- 위치 조정 설정 화면과 테스트 오버레이 구현
- 명함 상세는 기존 PWA URL을 WebView 또는 Custom Tab으로 열기

### 3단계: 운영 품질

- 재부팅 후 sync, 네트워크 실패 retry
- 이미지 캐시 용량 제한과 정리 정책
- 오류/성능 로그 수집
- One UI 8.5 이상 대상 기기별 QA 매트릭스 정리
- 관리자가 모바일 사용 기기를 폐기하는 화면 추가

## 예상 일정

- One UI 8.5 기술 검증: 2~4일
- 서버 동기화/이미지 API: 4~7일
- Android MVP: 10~15일
- 실제 Galaxy QA와 배포 준비: 5~10일

총 MVP는 한 명 기준 약 4~6주가 현실적이다. 명함 이미지 캐시, 가상 이미지 렌더러, 위치 조정 UX가 포함되므로 초기 설계보다 Android 구현 비중이 커진다.

## 주요 리스크

- 발신자 표시/전화 선별 역할은 사용자가 직접 선택해야 하며, 동일 역할을 쓰는 다른 앱과 동시에 활성화하지 못할 수 있다.
- `SYSTEM_ALERT_WINDOW`는 사용자가 꺼둘 수 있고, One UI 8.5에서도 전화 화면 위 표시 우선순위는 실제 기기 검증이 필요하다.
- 저장된 명함 사진이 원본 data URL로 크면 모바일 동기화 비용이 커질 수 있어 thumbnail/hash 기반 캐시 설계가 필요하다.
- 가상 명함 이미지를 Android에서 렌더링하면 `/my-card` Canvas 결과와 미세한 폰트/줄바꿈 차이가 생길 수 있다.
- 번호 매칭은 발신번호 표시 형식, 대표번호, 내선, 국제번호 표기 차이 때문에 100% 정확하지 않다.

## 참고 자료

- Android `CallScreeningService`: https://developer.android.com/reference/android/telecom/CallScreeningService
- Android `RoleManager.ROLE_CALL_SCREENING`: https://developer.android.com/reference/android/app/role/RoleManager
- Android overlay window 변경사항: https://developer.android.com/about/versions/oreo/android-8.0-changes
- Android background activity launch 제한: https://developer.android.com/guide/components/activities/background-starts
- Samsung One UI 8.5 rollout 공지: https://news.samsung.com/us/samsungs-one-ui-8-5-official-rollout-may-6/
- Samsung One UI 8.5 소개: https://www.samsung.com/us/apps/one-ui/
