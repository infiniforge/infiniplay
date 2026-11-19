plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.swordfish.libretrodroid"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    sourceSets["main"].java.srcDir("../third_party/libretrodroid/libretrodroid/src/main/java")
    sourceSets["main"].manifest.srcFile("AndroidManifest.xml")
}

dependencies { api("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2") }
