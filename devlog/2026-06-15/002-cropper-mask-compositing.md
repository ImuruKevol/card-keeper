# cropper 마스크 합성 안정화

- **ID**: 002
- **날짜**: 2026-06-15
- **유형**: 버그 수정

## 작업 요약
남아 있던 cropper 조작 중 미세 깜빡임을 줄이기 위해 고정 프레임의 대형 `box-shadow` 마스크를 제거했다. 프레임 주변 어두운 영역은 네 개의 정적 dim 레이어로 분리하고 초기화/리사이즈 때만 위치를 동기화해 이미지 transform 중 대형 그림자 repaint가 발생하지 않도록 했다.

## 원문 요청사항
```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

여전히 살짝 깜빡거림 현상이 남아있기는 해.

## 리뷰 요약

- 리뷰 ID: hhvzjpzvkwzscndvbjettkwjlbpumrwq
- 제목: 명함 촬영 후 깜빡거림
- 요청 링크: https://bus.sub.nanoha.kr/
- Codex 요청자: 권태욱
- 프로젝트 루트: /opt/app
- Codex 세션 ID: 019ec983-2d91-7383-88a7-8f08ae9fd5fd
- 스크린샷 컨텍스트: 없음
- 에이전트 작업 지시서 컨텍스트: 없음
- HTML 문서 생성 규칙 컨텍스트: 없음
- HTML 문서 설정 컨텍스트: 없음
- HTML 프로젝트 인스트럭션 파일: 없음
- 첨부파일 컨텍스트: 0개

## 세션 처리

저장된 Codex 세션을 resume해 이전 대화 맥락을 우선 사용하세요. 이전 Codex 히스토리는 이 요청에 포함되지 않습니다.
```

## 변경 파일 목록
- `src/app/page.cards/view.pug`: cropper stage에 top/right/bottom/left dim 레이어를 추가했다.
- `src/app/page.cards/view.scss`: 기존 대형 `box-shadow` 마스크를 제거하고 dim 레이어의 별도 합성/contain 스타일을 추가했다.
- `src/app/page.cards/view.ts`: cropper frame 기준으로 dim 레이어의 크기와 위치를 초기화/리사이즈 시 동기화하도록 추가했다.
- `devlog.md`: 작업 요약 행을 추가했다.
- `devlog/2026-06-15/002-cropper-mask-compositing.md`: 작업 상세 기록을 추가했다.

## 검증 결과
- `wiz_project_build(clean=false, projectName="main")`: 성공
- `PLAYWRIGHT_BASE_URL=http://127.0.0.1:3000 npm run test:e2e -- --project=chromium`: 성공, 3 passed
- 소스/빌드 산출물에서 cropper 대형 `box-shadow` 마스크가 제거되고 dim 레이어 및 `syncCropperMask()` 경로가 포함된 것을 확인했다.

## 남은 리스크
- 실제 모바일 PWA 카메라 조작 환경에서는 직접 재현 검증하지 못했다.
- 작업 전부터 존재하던 Android/PWA/devlog 관련 변경 파일들은 이번 작업 범위 밖이라 유지했다.
