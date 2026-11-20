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
        versionCode = 15
        versionName = "0.4.2"
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
    sourceSets["main"].jniLibs.srcDir(layout.buildDirectory.dir("standalone-cores/jni"))
    sourceSets["main"].assets.srcDir(layout.buildDirectory.dir("generated/core-assets"))
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
            file("scripts/apply-n64-bob.py"),
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
            file("scripts/mupen-asm-defines.py"),
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
                    "libpcsx.so",
                    "libdesmume.so",
                    "libmupen64plus.so",
                )
                .map { layout.buildDirectory.file("native/jni/arm64-v8a/$it") }
        )
    }

tasks.configureEach { if (name.startsWith("configureCMake")) dependsOn(prepareHost) }

val buildStandaloneCores by
    tasks.registering(Exec::class) {
        inputs.files(
            "scripts/build-standalone-cores.py",
            "third-party.lock.json",
            "src/main/cpp/standalone-overrides.cmake",
            "src/main/cpp/termux-compat/syslog.h",
        )
        outputs.files(
            layout.buildDirectory.file("standalone-cores/jni/arm64-v8a/libppsspp.so"),
            layout.buildDirectory.file("standalone-cores/jni/arm64-v8a/libcitra.so"),
        )
        doFirst {
            commandLine(
                if (termuxNative) listOf("python3", "scripts/build-standalone-cores.py")
                else
                    listOf(
                        "python3",
                        "scripts/build-standalone-cores.py",
                        "--ndk",
                        androidComponents.sdkComponents.ndkDirectory.get().asFile.absolutePath,
                    )
            )
        }
    }
val prepareCoreAssets by
    tasks.registering(Exec::class) {
        commandLine("python3", "scripts/prepare-core-assets.py")
        inputs.files("scripts/prepare-core-assets.py", "third-party.lock.json")
        inputs.dir("third_party/ppsspp/assets")
        outputs.dir(layout.buildDirectory.dir("generated/core-assets"))
    }

tasks.named("preBuild").configure {
    dependsOn(buildStandaloneCores, prepareCoreAssets)
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
