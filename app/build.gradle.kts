import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    jacoco
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
            // These values are read from local.properties for local builds
            // CI/CD uses GitHub Secrets instead
            val keystoreFile = rootProject.file(localProperties.getProperty("keystore.file"))
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = System.getenv("KEY_STORE_PASSWORD") ?: localProperties.getProperty("keystore.password")
                keyAlias = System.getenv("KEY_ALIAS") ?: localProperties.getProperty("key.alias")
                keyPassword = System.getenv("KEY_PASSWORD") ?: localProperties.getProperty("key.password")
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
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
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

tasks.withType<Test> {
    configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

val jacocoTestReport = tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("testDebugUnitTest"))

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val kotlinClasses = fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
        exclude("**/R.class")
        exclude("**/R\$*.class")
        exclude("**/BuildConfig.*")
        exclude("**/Manifest*.*")
        exclude("**/*Test*.*")
        exclude("android/**/*.*")
    }

    val javaClasses = fileTree(layout.buildDirectory.dir("intermediates/javac/debug")) {
        exclude("**/R.class")
        exclude("**/R\$*.class")
        exclude("**/BuildConfig.*")
        exclude("**/Manifest*.*")
        exclude("**/*Test*.*")
        exclude("android/**/*.*")
    }

    val mainSrc = "${project.projectDir}/src/main/java"

    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(kotlinClasses, javaClasses))
    executionData.setFrom(files(layout.buildDirectory.file("jacoco/testDebugUnitTest.exec")))
}