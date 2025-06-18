import java.util.Properties
// Test and Jacoco imports are fine, only used if tasks are invoked.

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22" // Explicit Plugin ID
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
    id("com.google.dagger.hilt.android")
    id("io.gitlab.arturbosch.detekt") version "1.23.5"
    id("jacoco")
}

android {
    namespace = "com.familystalking.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.familystalking.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        val properties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(localPropertiesFile.inputStream())
        }

        buildConfigField("String", "SUPABASE_URL", "\"${properties.getProperty("SUPABASE_URL", "")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${properties.getProperty("SUPABASE_ANON_KEY", "")}\"")
        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] = properties.getProperty("GOOGLE_MAPS_API_KEY", "")
    }

    buildTypes {
        release {
            isMinifyEnabled = false // Consider 'true' for actual release builds
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        compose = true
        buildConfig = true // Correct for enabling BuildConfig
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8" // Ensure compatibility with Kotlin 1.9.22
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("com.google.ar:core:1.49.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    // Define versions in one place for better management
    val composeBomVersion = "2024.05.00" // <-- THIS IS THE ONLY CHANGE
    val hiltVersion = "2.50"
    val supabaseVersion = "2.1.3"
    val lifecycleVersion = "2.7.0"

    val cameraxVersion = "1.4.2"
    val detektPluginVersion = "1.23.5"
    val accompanistVersion = "0.31.5-beta"
    val zxingVersion = "3.5.2"


    // Core & Lifecycle
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycleVersion")

    // Compose
    implementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.runtime:runtime-livedata:1.6.6")

    // Accompanist Permissions (for CameraScreen.kt if using Accompanist)
    implementation("com.google.accompanist:accompanist-permissions:$accompanistVersion")

    // CameraX
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    // For ImageAnalysis and QR Code scanning
    implementation("com.google.zxing:core:$zxingVersion")

    // Supabase

    implementation(platform("io.github.jan-tennert.supabase:bom:$supabaseVersion"))
    implementation("io.github.jan-tennert.supabase:gotrue-kt")
    implementation("io.github.jan-tennert.supabase:compose-auth-ui")
    implementation("io.github.jan-tennert.supabase:compose-auth")
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")
    implementation("io.github.jan-tennert.supabase:functions-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")

    // Ktor (Supabase BOM might manage versions, but explicit is fine)
    implementation("io.ktor:ktor-client-android:2.3.8")
    implementation("io.ktor:ktor-client-core:2.3.8")
    implementation("io.ktor:ktor-client-cio:2.3.8") // Keep if it doesn't cause issues and Supabase might use it
    implementation("io.ktor:ktor-client-okhttp:2.3.8") // Added OkHttp engine
    implementation("io.ktor:ktor-client-logging:2.3.8") // Added Logging plugin

    // KotlinX Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
    // implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.2") // Usually brought by -json

    // Hilt for Dependency Injection
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    kapt("com.google.dagger:hilt-android-compiler:$hiltVersion")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Maps & Location
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.maps.android:maps-compose:4.3.3")
    implementation("com.google.maps.android:maps-compose-utils:4.3.3")

    // DataStore Preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0") // Or latest stable 1.0.x / 1.1.x

    // Detekt
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:$detektPluginVersion")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

detekt {
    buildUponDefaultConfig = true
    allRules = false // Only rules active in your config will run (plus defaults not overridden)
    config.setFrom(files("$rootDir/config/detekt/detekt.yml")) // Path to your Detekt config
    baseline = file("$rootDir/config/detekt/baseline.xml") // Path to your baseline file
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(false)
        sarif.required.set(true)
    }
}

// Jacoco configuration - kept as you had it
tasks.withType<Test> {
    configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

tasks.register<JacocoReport>("coverage") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    val fileFilterPatterns = listOf(
        "**/R.class", "**/R\$*.class", "**/BuildConfig.*", "**/Manifest*.*",
        "**/*Test*.*", "android/**/*.*",
        "**/*\$*COROUTINE\$*", "**/*\$Lambda\$*.*", "**/*\$inlined\$*.*",
        "**/*\$DefaultImpls.*", "**/*\$Companion$*.*", "**/*Kt.class",
        "**/models/**", "**/di/**"
    )

    val classDirectoriesForCoverage = project.layout.buildDirectory.map { buildDir ->
        listOf(
            buildDir.file("tmp/kotlin-classes/debug"),
            buildDir.file("intermediates/javac/debug/classes")
        )
    }.map { dirs ->
        project.files(dirs).asFileTree.matching {
            exclude(fileFilterPatterns)
        }
    }
    classDirectories.setFrom(classDirectoriesForCoverage)

    val sourceDirectoriesForCoverage = files(
        "${project.projectDir}/src/main/java",
        "${project.projectDir}/src/main/kotlin"
    )
    sourceDirectories.setFrom(sourceDirectoriesForCoverage)

    val executionDataForCoverage = project.layout.buildDirectory.map { buildDir ->
        project.fileTree(buildDir) {
            include(
                "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
                "jacoco/testDebugUnitTest.exec"
            )
        }
    }
    executionData.setFrom(executionDataForCoverage)
}