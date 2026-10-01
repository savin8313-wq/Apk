plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// شماره‌ی اجرای گیت‌هاب اکشن رو به‌عنوان کد نسخه استفاده می‌کنیم تا خود اپ
// بتونه با گرفتن آخرین Release از گیت‌هاب، بفهمه نسخه‌ی جدیدتری منتشر شده یا نه.
// وقتی محلی (روی خود سیستم/گوشی، نه توی گیت‌هاب اکشن) بیلد می‌شه، پیش‌فرض ۱ می‌مونه.
val ciRunNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toIntOrNull() ?: 1

android {
    namespace = "com.stockgrowth.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.stockgrowth.app"
        minSdk = 24
        targetSdk = 34
        versionCode = ciRunNumber
        versionName = "1.0.$ciRunNumber"
    }

    // کیستور امضای دیباگ رو صراحتاً از فایل ثابت داخل پروژه می‌خونیم (نه کیستور
    // خودکار هر ماشین)، چون هر ران گیت‌هاب اکشن یه سیستم تازه‌ست و بدون این کار
    // هر بار یه امضای متفاوت می‌ساخت که باعث خطای «تداخل بسته» موقع نصب آپدیت می‌شد.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")

    // شبکه
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // تست
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
}
