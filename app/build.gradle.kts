plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.litejoin"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.litejoin"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    // 뷰 바인딩 활성화
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // BOM (Bill of Materials)을 사용하여 Firebase 라이브러리 버전 관리
    implementation(platform("com.google.firebase:firebase-bom:33.0.0")) // 최신 버전 사용 권장
    // 1. Firebase Authentication (인증)
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.android.gms:play-services-auth:21.0.0") // Google 로그인 지원
    // 2. Firestore (게시글, 사용자 정보)
    implementation("com.google.firebase:firebase-firestore-ktx")
    // 3. Realtime Database (채팅)
    implementation("com.google.firebase:firebase-database-ktx")
    // 4. Firebase Storage (이미지 저장)
    implementation("com.google.firebase:firebase-storage-ktx")
    // 5. 기타 유용한 라이브러리 (코루틴 지원)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    // 서클 이미지 뷰 라이브러리
    implementation("de.hdodenhof:circleimageview:3.1.0")
    // Glide (이미지 로딩 라이브러리)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    // 새로고침 라이브러리
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
}