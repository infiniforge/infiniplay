plugins {
    id("com.android.application") version "8.9.2"
    id("org.jetbrains.kotlin.android") version "2.2.20"
}

val termuxNative = providers.gradleProperty("termuxNative").orNull == "true"

android {
    namespace = "com.infiniplay"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.infiniplay"
        minSdk = 26
        targetSdk = 35
        versionCode = 11
        versionName = "0.3.0"
        ndk { abiFilters.add("arm64-v8a") }
        if (!termuxNative) externalNativeBuild { cmake { arguments += "-DANDROID_STL=c++_static" } }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }
    if (termuxNative) sourceSets["main"].jniLibs.srcDir(layout.buildDirectory.dir("native/jni"))
    else
        externalNativeBuild {
            cmake {
                path = file("src/main/cpp/CMakeLists.txt")
                version = "3.22.1"
            }
        }
    packaging {
        jniLibs.useLegacyPackaging = true
        resources.merges += "META-INF/services/io.github.gretchycat.infinistream.CapabilityProvider"
    }
}

val prepareHost by
    tasks.registering(Exec::class) {
        commandLine("python3", "scripts/prepare-native.py")
        inputs.files(
            file("scripts/prepare-native.py"),
            file("scripts/prepare-extra-cores.py"),
            file("third-party.lock.json"),
            file("src/main/cpp/play_screens.h"),
            file("src/main/cpp/play_keyboard.h"),
        )
        outputs.files(
            listOf(
                    "video.cpp",
                    "input.cpp",
                    "quicknes-sources.cmake",
                    "gambatte-sources.cmake",
                    "fceumm-sources.cmake",
                    "extra-cores.cmake",
                )
                .map { layout.buildDirectory.file("native-source/$it") }
        )
    }
val buildTermuxNative by
    tasks.registering(Exec::class) {
        dependsOn(prepareHost)
        inputs.files(prepareHost)
        commandLine("python3", "scripts/build-native.py")
        inputs.files(
            file("src/main/cpp/CMakeLists.txt"),
            file("src/main/cpp/play_screens.h"),
            file("scripts/build-native.py"),
            file("src/main/cpp/fds_adapter.c"),
            file("src/main/cpp/fceumm_adapter.c"),
            file("src/main/cpp/pce_adapter.c"),
            file("src/main/cpp/ngp_adapter.c"),
            file("src/main/cpp/play_keyboard.h"),
        )
        outputs.files(
            listOf(
                    "liblibretrodroid.so",
                    "libquicknes.so",
                    "libgambatte.so",
                    "libfceumm.so",
                    "libc++_shared.so",
                    "libgenesis.so",
                    "libpicodrive.so",
                    "libpce.so",
                    "libhandy.so",
                    "libstella.so",
                    "libatari800.so",
                    "libprosystem.so",
                    "libsnes9x.so",
                    "libvb.so",
                    "libgeolith.so",
                    "libmgba.so",
                    "libngp.so",
                    "libwswan.so",
                )
                .map { layout.buildDirectory.file("native/jni/arm64-v8a/$it") }
        )
    }

tasks.configureEach { if (name.startsWith("configureCMake")) dependsOn(prepareHost) }

tasks.named("preBuild").configure {
    dependsOn(if (termuxNative) buildTermuxNative else prepareHost)
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation(project(":play-core"))
    implementation(project(":retro-host"))
    implementation("io.github.gretchycat.libinfiniselect:select-android:0.2.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    testImplementation("junit:junit:4.13.2")
}
