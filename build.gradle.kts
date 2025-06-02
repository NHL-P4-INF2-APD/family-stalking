buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.50")
        classpath("com.google.android.libraries.mapsplatform.secrets-gradle-plugin:secrets-gradle-plugin:2.0.1")
        classpath("org.jetbrains.kotlin:kotlin-serialization:1.9.22")
    }
}

plugins {
    id("com.android.application") version "8.2.2" apply false
    kotlin("android") version "1.9.22" apply false
    kotlin("kapt") version "1.9.22" apply false
    id("com.google.dagger.hilt.android") version "2.50" apply false
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1" apply false
    kotlin("plugin.serialization") version "1.9.22" apply false
}

group = "org.example"
version = "1.0-SNAPSHOT"

tasks.register("clean", Delete::class) {
    delete(buildDir)
}