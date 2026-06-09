# README 현행화 및 문서용 스크린샷·민감 정보 정리

- **ID**: 007
- **날짜**: 2026-06-09
- **유형**: 문서 수정 / 보안 정리 / 자산 추가

## 작업 요약

현재 명함 관리 서비스의 개발 상태에 맞게 README를 다시 정리하고, ReviewOps 첨부 스크린샷을 문서용 자산으로 추가했다. 명함 목록 스크린샷의 실제 이름, 회사, 연락처는 더미 데이터로 익명화했고, 가져오기 화면의 파일명도 샘플명으로 교체했다. tracked 문서에 남아 있던 샘플 DB 비밀번호 문구를 placeholder로 바꾸고 `.gitignore`에 로컬 secret 및 DB 파일 패턴을 보강했다.

## 원문 요청사항

```text
작업 시작

readme를 현재 개발 상황에 맞춰서 업데이트해줘.
git에 민감 정보가 올라가지 않도록 git rm cached 등 명령어를 활용해서 잘 필터링해줘. 필요하면 기존 git commit들을 하나로 합치거나 .git을 삭제 후 필터링한 다음 다시 git add, commit을 해도 돼.
스크린샷을 찍어서 첨부했으니 이 스크린샷들도 활용해줘. 근데 2_명함목록.png는 아랫부분의 명함 목록에 내가 등록한 명함들이 있으니 그 사람들의 정보를 다른 이름, 회사, 번호들로 알아서 잘 바꿔서 수정해서 사용해야해.
```

## 변경 파일 목록

### 문서

- `README.md`: 현재 화면, 기능, OCR/AI fallback, 가져오기/내보내기, API, 검증 방법, 민감 정보 관리 기준으로 재작성
- `devlog.md`: 이번 작업 요약 행 추가
- `devlog/2026-06-09/007-readme-sensitive-assets.md`: 이번 작업 상세 기록 추가
- `devlog/2026-06-04/001-business-card-skeleton.md`: 과거 원문 예시의 DB 비밀번호를 `<local-secret>` placeholder로 마스킹

### 보안/무시 규칙

- `.gitignore`: `.env.*`, 로컬 DB 파일, `data/`, key/pem/secret 파일 패턴 추가

### README 자산

- `docs/screenshots/login.png`: 로그인 화면 스크린샷 추가
- `docs/screenshots/card-photo-register.png`: 사진 분석 등록 화면 스크린샷 추가
- `docs/screenshots/card-import.png`: 가져오기 화면 스크린샷 추가 및 파일명 샘플화
- `docs/screenshots/card-list-sanitized.png`: 명함 목록 화면 스크린샷 추가 및 하단 명함 정보 익명화

## 검증 결과

- `python tests/import_export_smoke.py`: 통과
- `git grep` 기반 secret 패턴 재검색: 실제 DB 비밀번호, OpenAI/Google API Key, private key 패턴 잔여 없음
- `git ls-files -ci --exclude-standard`: ignored 상태인데 tracked인 파일 없음
- `git diff --check`: 공백 오류 없음
- 익명화된 `docs/screenshots/card-list-sanitized.png` 시각 확인: 실제 이름, 회사, 전화번호 대신 더미 데이터 표시 확인
- `docs/screenshots/card-import.png` 시각 확인: 첨부 원본 파일명 대신 `sample_cards.xlsx` 표시 확인

## 남은 리스크

- 이번 작업은 문서/자산 중심이라 전체 Playwright 브라우저 테스트와 WIZ 빌드는 실행하지 않았다.
- 과거 git commit history 자체는 rewrite하지 않았다. 현재 tracked 파일 기준으로 secret 잔여와 tracked ignored 파일이 없음을 확인했다.
- AI Provider API Key는 조회 응답에서 원문을 반환하지 않지만 운영 DB와 백업에는 저장될 수 있으므로 별도 secret 관리 정책이 필요하다.
