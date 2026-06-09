plugins {
    id("com.android.application")
}

android {
    namespace = "kr.nanoha.buscard.caller"
    compileSdk = 36

    defaultConfig {
        applicationId = "kr.nanoha.buscard.caller"
        minSdk = 36
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "WEB_BASE_URL", "\"https://bus.sub.nanoha.kr/\"")
    }

    buildFeatures {
        buildConfig = true
    }
}
