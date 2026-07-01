import os

APK_PATH = "android/app/build/outputs/apk/debug/app-debug.apk"
DOWNLOAD_NAME = "business-card-caller-0.1.1.apk"

apk_path = wiz.project.fs().abspath(APK_PATH)
if not os.path.exists(apk_path):
    wiz.response.status(404, message="다운로드할 Android APK 파일을 찾을 수 없습니다.")

wiz.response.download(apk_path, as_attachment=True, filename=DOWNLOAD_NAME)
