# DB 라운드로빈 접속 불안정으로 인한 로그인/API 실패 완화

- **ID**: 002
- **날짜**: 2026-06-10
- **유형**: 버그 수정

## 작업 요약

`db` 호스트가 두 개의 MariaDB 주소로 해석되고, 그중 한 주소가 `wiz` 계정 인증을 거부해 로그인과 DB 사용 API가 간헐적으로 500 오류를 내는 상태를 확인했다. 프로젝트 DB 설정에서 MySQL 계열 DB가 복수 주소로 해석될 때 실제 인증 가능한 호스트를 한 번 탐지하고 서버 캐시에 저장해 이후 같은 프로세스에서는 안정적으로 해당 호스트를 사용하도록 보강했다.

## 원문 요청사항

```text
서버 쪽에 문제가 있는건지 어디에 문제가 있는건지 로그인도 되다가 안되다가 하고, API 호출도 되다가 안되다가 하고 있어. 확인해줘.
```

## 변경 파일 목록

- `config/database.py`
  - `DB_HOST`가 복수 IP로 해석되는 경우 각 후보에 짧은 MySQL 인증 연결을 시도해 성공하는 호스트를 선택하도록 추가했다.
  - 선택된 호스트를 WIZ 서버 캐시에 저장해 요청마다 DNS 라운드로빈을 다시 타지 않도록 했다.
  - `DB_HOST_AUTODISCOVER=false`로 탐지 동작을 끌 수 있게 했다.
- `bundle/config/database.py`
  - WIZ 빌드를 통해 운영 번들 설정에 동일 변경을 반영했다.
- `devlog.md`
  - 작업 요약 행을 추가했다.
- `devlog/2026-06-10/002-db-host-autodiscovery-login-api-stability.md`
  - 작업 상세 기록을 추가했다.

## 검증 결과

- 원인 확인
  - `db` DNS가 `10.0.2.76`, `10.0.2.86` 두 주소로 해석됨을 확인했다.
  - 동일 계정으로 `10.0.2.76` 접속은 성공하고, `10.0.2.86` 접속은 `Access denied for user 'wiz'@'10.0.2.78'`로 실패함을 확인했다.
  - `/opt/app/wiz.log`에서 `/wiz/api/page.access/login`, `/wiz/api/page.my_card/load`의 동일 DB 인증 실패 스택트레이스를 확인했다.
- `python -m py_compile config/database.py` 성공
- 실행 중 서버 환경 변수 기준 `config/database.py` 로드 테스트에서 인증 가능한 호스트가 선택됨을 확인했다.
- `wiz_project_build(clean=false)` 성공
- 배포 URL 검증
  - `/auth/check` 20회 연속 `200`
  - `/api/mobile/health` 20회 연속 `200`
  - 잘못된 계정으로 `/wiz/api/page.access/login` 30회 호출 시 모두 앱 레벨 `401` 응답, DB 접속 `500` 재현 없음
  - 동일 로그인 API를 devmode 쿠키 없이 10회 추가 호출해 모두 앱 레벨 `401` 응답 확인

## 남은 리스크

- 근본 원인은 `db` 뒤의 한 MariaDB 인스턴스 또는 DNS 대상의 계정/권한 불일치다. 해당 인스턴스의 `wiz@%` 계정과 비밀번호/권한을 맞추거나 DNS에서 제외하는 인프라 조치가 필요하다.
- 현재 수정은 애플리케이션 프로세스 단위 완화책이므로, 인증 가능한 DB 인스턴스가 내려가면 서버 재시작 또는 인프라 정리가 필요할 수 있다.
