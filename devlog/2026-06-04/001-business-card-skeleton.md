# 명함 관리 서비스 초기 스켈레톤 및 DB 연동 설정

- **ID**: 001
- **날짜**: 2026-06-04
- **유형**: 기능 추가 / 설정 변경 / 리팩토링

## 작업 요약
MariaDB 환경 변수 기반 DB 설정을 추가하고, 사용자 승인 기반 인증 흐름과 명함 CRUD 스켈레톤을 구성했다.
기존 게시물 샘플 앱과 post 패키지를 제거하고, 대시보드/사이드바/README를 명함 관리 서비스 기준으로 정리했다.

## 원문 요청사항
```text
작업 시작

아래 정보를 참고해서 DB 연동 설정을 해줘. 그리고 나는 명함 관리 서비스를 만들고 싶어.
기본적으로 "리멤버" 앱의 명함 관리 부분만 따로 내 전용 서비스를 만드는 느낌으로 개발하고 싶어.
사용자 관리 기능은 기본적인 기능만 있으면 되고, 회원가입 후 반드시 관리자의 승인이 있어야 이용을 할 수 있게 하고 싶어.
현재 존재하는 샘플 코드들 중 필요 없는 부분을 전부 정리하고 스켈레톤을 구성해줘.
---
    environment:
      LANG: C.UTF-8
      TZ: Asia/Seoul
      DB_TYPE: mariadb
      DB_HOST: db
      DB_PORT: '3306'
      DB_NAME: wiz
      DB_USER: wiz
      DB_PASSWORD: business@2026
```

## 변경 파일 목록

### 설정
- `config/database.py`: MariaDB/MySQL 환경 변수 기반 DB 연결 설정 추가
- `config/season.py`: 로그인 URI, PWA 기본값, 세션 생성/사용자 ID 콜백 구현

### 모델/인증
- `src/model/db/user.py`: 승인 상태와 마지막 접속 필드를 포함한 사용자 테이블 정의
- `src/model/db/user_session.py`: 세션 토큰 저장 테이블 정의
- `src/model/db/access_log.py`: 로그인/로그아웃 로그 테이블 정의
- `src/model/db/business_card.py`: 명함 테이블 정의
- `src/model/struct.py`: 사용자/명함/세션/접속 로그 Struct 연결
- `src/model/struct/user.py`: 가입 신청, 첫 관리자 부트스트랩, 승인/차단/권한 변경, 인증 구현
- `src/model/struct/user_session.py`: 세션 토큰 등록/검증/비활성화 구현
- `src/model/struct/access_log.py`: 접속 로그 기록/조회 구현
- `src/model/struct/business_card.py`: 명함 검색/등록/수정/삭제/통계 구현
- `src/controller/user.py`: 로그인, 활성 계정, 세션 토큰 검증 추가
- `src/controller/admin.py`: 관리자 권한 검증 유지
- `src/portal/season/route/auth/controller.py`: `/auth/check`, `/auth/logout` 세션 검증 및 로그아웃 처리 보강

### UI/API
- `src/app/page.access/*`: 로그인/가입 신청 화면과 API 재구성
- `src/app/page.dashboard/*`: 명함 현황 대시보드로 교체
- `src/app/page.cards/*`: 명함 CRUD 페이지/API 신규 추가
- `src/app/page.users/*`: 관리자 전용 사용자 승인/관리 페이지/API 신규 추가
- `src/app/component.nav.sidebar/*`: 명함/사용자 중심 사이드바로 정리
- `src/assets/lang/ko.json`, `src/assets/lang/en.json`: 메뉴 번역 정리

### 문서/정리
- `README.md`: 명함 관리 서비스 스켈레톤 문서로 교체
- `src/app/page.posts/`: 삭제
- `src/app/page.posts.item/`: 삭제
- `src/app/page.members/`: 삭제
- `src/portal/post/`: 삭제
- `src/angular/styles/styles/sample.scss`: 삭제

## 검증 결과
- `wiz_project_build(clean=true)`: 성공
- `wiz_project_build(clean=false)`: 성공
- 개발 쿠키 `season-wiz-project=main`, `season-wiz-devmode=true` 포함 검증:
  - `GET https://bus.sub.nanoha.kr/access`: HTTP 200
  - `GET https://bus.sub.nanoha.kr/dashboard`: HTTP 200
  - `GET https://bus.sub.nanoha.kr/auth/check`: HTTP 200, `status=false`
  - `GET https://bus.sub.nanoha.kr/wiz/api/page.cards/list`: WIZ 응답 코드 401
- 샘플 게시물/post 참조 검색: 잔여 참조 없음

## 남은 리스크
- 실제 MariaDB 컨테이너 접속과 테이블 생성은 런타임 DB 상태에 의존한다.
- 기존 DB에 같은 테이블이 이미 있으면 `create_table(safe=True)`는 신규 컬럼 마이그레이션을 수행하지 않는다.
- 첫 계정은 관리자 승인 기능을 시작하기 위한 부트스트랩 예외로 즉시 활성 관리자 계정이 된다.
