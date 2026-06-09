# 022. 내 명함 모던 베이스 배경 도형 절제

## 사용자 요청

- 모던 디자인의 배경 도형들이 너무 과하므로 줄인다.

## 변경 파일

- `src/app/page.my_card/view.ts`
  - `모던` 베이스의 중앙/좌측 강한 도형을 제거했다.
  - 낮은 투명도의 좌측 패널, 하단 얇은 색 면, 우측 보조 면만 남겨 배경을 더 절제된 형태로 조정했다.
- `devlog.md`
- `devlog/2026-06-09/022-my-card-modern-background-simplify.md`

## 확인 결과

- `wiz_project_build(clean=false)` 성공.
- `git diff --check` 성공.
- `npm run test:e2e` 성공: 6개 테스트 통과.
- Playwright 모바일 412px에서 `모던` 베이스 렌더링을 캡처로 확인했다.

## 남은 리스크

- 실제 선호도는 주관적이므로, 더 미니멀한 방향이 필요하면 추가 조정이 필요할 수 있다.
