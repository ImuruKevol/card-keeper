# 내 명함 모바일 화면 Android APK 다운로드 버튼 추가

- ID: 027
- 날짜: 2026-06-09
- 유형: ReviewOps 요청 처리

## 작업 요약

내 명함 모바일 화면 하단에서 Android APK를 직접 다운로드할 수 있도록 공개 다운로드 라우트와 모바일 전용 다운로드 버튼을 추가했다. 라우트는 현재 프로젝트의 debug APK 빌드 산출물을 `business-card-caller.apk` 파일명으로 내려준다.

## 원문 요청사항

```text
# ReviewOps Codex 작업 요청

아래 요청을 현재 프로젝트 루트에서 처리하세요. 필요한 파일을 직접 수정하고, 마지막 응답은 한국어로 간결하게 작성하세요.
스트리밍 응답은 사용하지 않습니다. 작업이 끝난 뒤 변경 요약, 확인한 내용, 남은 리스크만 정리하세요.
이 작업의 세션 단위는 아래 리뷰 ID입니다. 리뷰 ID가 같으면 같은 Codex 히스토리 맥락으로 이어서 처리하세요.

## 사용자 요청

apk는 모바일 화면에서 다운받을 수 있게 해줘. 일단 내 명함 화면 제일 아래에 다운로드받을 수 있는 버튼을 추가하면 될 것 같아.

## 리뷰 요약

- 리뷰 ID: ybauiwwadospkltesipbfwwcnfcdnqyu
- 제목: 안드로이드 앱 개발
- 요청 링크: https://bus.sub.nanoha.kr/
- Codex 요청자: 권태욱
- 프로젝트 루트: /opt/app
- Codex 세션 ID: 019eab86-cd6d-7872-994e-a574aebc5f4c
- 스크린샷 컨텍스트: 없음
- 에이전트 작업 지시서 컨텍스트: 없음
- HTML 문서 생성 규칙 컨텍스트: 없음
- HTML 문서 설정 컨텍스트: 없음
- HTML 프로젝트 인스트럭션 파일: 없음
- 첨부파일 컨텍스트: 0개
```

## 변경 파일

- `src/route/android-apk-download/app.json`
- `src/route/android-apk-download/controller.py`
- `src/app/page.my_card/view.pug`
- `src/app/page.my_card/view.scss`
- `devlog.md`
- `devlog/2026-06-09/027-my-card-mobile-apk-download.md`

## 검증 결과

- `python3 -m compileall src/route/android-apk-download` 성공
- `./gradlew :app:assembleDebug` 성공
- `wiz_project_build(projectName="main", clean=false)` 성공
- `curl`로 `http://localhost:3000/download/android-app.apk` 다운로드 확인
  - 쿠키: `season-wiz-project=main; season-wiz-devmode=true`
  - 응답: `200 application/vnd.android.package-archive 2614305`
  - `Content-Disposition: attachment; filename=business-card-caller.apk`
- 다운로드 파일과 원본 APK SHA256 일치 확인
  - `9d824c3b899a0f294e4ab0e480f5c4f394836283bc81c506b5f0c85df1836497`
- `rg`로 소스와 번들에 `Android 앱 다운로드`, `/download/android-app.apk` 반영 확인

## 남은 리스크

- 다운로드 라우트는 현재 debug APK 빌드 산출물(`android/app/build/outputs/apk/debug/app-debug.apk`)에 의존하므로, 배포 환경에 해당 파일이 없으면 404를 반환한다.
- 다운로드 라우트는 모바일 사용자가 바로 받을 수 있도록 공개 라우트로 구성했다.
- 실제 Android 기기에서 브라우저 다운로드 및 설치 흐름은 별도 기기 테스트가 필요하다.
