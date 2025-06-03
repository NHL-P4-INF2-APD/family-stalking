// settings.gradle.kts (Project Root)

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage") // This is fine if you are using a feature marked as unstable
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS) // PREFER_SETTINGS is good
    repositories {
        google()
        mavenCentral()
        // If you have other custom repos like JitPack, add them here
    }
}

rootProject.name = "family-stalking"
include(":app")