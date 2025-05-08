import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    jacoco
    id("io.gitlab.arturbosch.detekt")
}

// Load local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    FileInputStream(localPropertiesFile).use { localProperties.load(it) }
}

android {
    namespace = "com.example.familystalker"
    compileSdk = 34

    signingConfigs {
        create("release") {
            // CI environment check
            if (System.getenv("CI") == "true") {
                // CI/CD uses GitHub Secrets
                storeFile = null // Will be handled by r0adkll/sign-android-release action
                storePassword = System.getenv("KEY_STORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            } else {
                // Local development signing
                val keystoreFilePath = localProperties.getProperty("keystore.file")
                if (keystoreFilePath != null) {
                    storeFile = rootProject.file(keystoreFilePath)
                    storePassword = localProperties.getProperty("keystore.password")
                    keyAlias = localProperties.getProperty("key.alias")
                    keyPassword = localProperties.getProperty("key.password")
                }
            }
        }
    }

    defaultConfig {
        applicationId = "com.example.familystalker"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // Disable Android's built-in JaCoCo integration
            enableUnitTestCoverage = false
            enableAndroidTestCoverage = false
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (System.getenv("CI") != "true" && !localProperties.containsKey("keystore.file")) {
                null
            } else {
                signingConfigs.getByName("release")
            }
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
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// JaCoCo configuration
jacoco {
    toolVersion = "0.8.11"
}

// Configure JaCoCo agent
tasks.withType<Test>().configureEach {
    jacoco {
        setExcludes(setOf("jdk.internal.*"))
    }
}

// Create a task to generate coverage report
tasks.register<JacocoReport>("generateCoverageReport") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val excludes = setOf(
        "**/R.class",
        "**/R\$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*"
    )

    val debugTree = fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
        exclude(excludes)
    }

    val mainSrc = "${project.projectDir}/src/main/java"
    val mainKotlin = "${project.projectDir}/src/main/kotlin"

    sourceDirectories.setFrom(files(mainSrc, mainKotlin))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(fileTree(layout.buildDirectory) {
        include("jacoco/testDebugUnitTest.exec")
    })
}

// Alias task
tasks.register("coverage") {
    dependsOn("testDebugUnitTest", "generateCoverageReport")
}