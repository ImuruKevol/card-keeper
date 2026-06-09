# AI 설정 메뉴명 변경

- **ID**: 007
- **날짜**: 2026-06-08
- **유형**: UI 문구 수정

## 작업 요약
AI OCR 설정으로 표시되던 관리자 메뉴와 AI 설정 페이지 제목을 AI 설정으로 변경했다.
저장 성공 메시지와 ko/en 내비게이션 번역 키도 같은 명명으로 정리했다.

## 원문 요청사항
```text
작업 시작

AI OCR 설정 -> AI 설정
으로 수정해줘.
```

## 변경 파일 목록
- `src/app/layout.sidebar/view.pug`: 관리자 내비게이션 메뉴 라벨을 `AI 설정`으로 변경
- `src/app/page.ai_settings/view.pug`: 페이지 제목을 `AI 설정`으로 변경
- `src/app/page.ai_settings/view.ts`: 저장 성공 메시지를 `AI 설정이 저장되었습니다.`로 변경
- `src/assets/lang/ko.json`: `nav.aiSettings` 라벨을 `AI 설정`으로 변경
- `src/assets/lang/en.json`: `nav.aiSettings` 라벨을 `AI Settings`로 변경
- `devlog.md`: 작업 요약 행 추가
- `devlog/2026-06-08/007-ai-settings-menu-label.md`: 작업 상세 기록 추가

## 검증 결과
- `rg -n "AI OCR 설정|AI 설정|AI Settings" src/app src/assets/lang`: 변경 대상 라벨 반영 및 구 라벨 미노출 확인
- `wiz_project_build(clean=false)`: 성공
- `curl`에 `session`, `season-wiz-project=main`, `season-wiz-devmode=true` 쿠키를 추가해 `page.ai_settings/get_setting` API 200 응답 확인
- Playwright로 `http://127.0.0.1:3000/ai-settings`에 동일 쿠키를 추가하고 `AI 설정` heading/nav 표시 및 `AI OCR 설정` 미노출 확인

## 남은 리스크
- 실제 운영 반영 여부는 배포 파이프라인에 의존한다.
