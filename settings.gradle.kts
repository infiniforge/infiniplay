pluginManagement {
    resolutionStrategy.eachPlugin {
        if (requested.id.id.startsWith("com.android."))
            useModule("com.android.tools.build:gradle:8.9.2")
    }
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "InfiniPlay"

include(":play-core", ":retro-host")

System.setProperty("infini.dependencies", file("libraries").canonicalPath)

includeBuild("libraries/libinfinistream")

includeBuild("libraries/libinfiniselect")
