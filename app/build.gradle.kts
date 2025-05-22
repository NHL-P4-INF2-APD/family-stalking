import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    kotlin("plugin.serialization") version "1.9.22"
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
            isMinifyEnabled = false
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
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBomVersion = "2024.02.00"
    val hiltVersion = "2.50"
    val supabaseVersion = "2.1.3"
    val lifecycleVersion = "2.7.0"
    val cameraxVersion = "1.3.1"
    val detektVersion = "1.23.5"

    implementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycleVersion")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.runtime:runtime-livedata:1.6.6")

    // Supabase
    implementation(platform("io.github.jan-tennert.supabase:bom:$supabaseVersion"))
    implementation("io.github.jan-tennert.supabase:gotrue-kt")
    implementation("io.github.jan-tennert.supabase:compose-auth-ui")
    implementation("io.github.jan-tennert.supabase:compose-auth")
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")
    implementation("io.github.jan-tennert.supabase:functions-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")
    implementation("io.ktor:ktor-client-android:2.3.8")
    implementation("io.ktor:ktor-client-core:2.3.8")
    implementation("io.ktor:ktor-client-cio:2.3.8")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.2")

    // Hilt
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    kapt("com.google.dagger:hilt-android-compiler:$hiltVersion")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Google Maps and Location
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.1.0")
    implementation("com.google.maps.android:maps-compose:4.3.0")

    // CameraX
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // ZXing (Barcode/QR scanning)
    implementation("com.google.zxing:core:3.5.3")

    // Accompanist
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Detekt
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:$detektVersion")

    // Google Maps SDK and Compose integration
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.maps.android:maps-compose:2.11.4")

    // ZXing dependency for QR-code generation
    implementation("com.google.zxing:core:3.5.3")

    // Accompanist Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    baseline = file("$rootDir/config/detekt/baseline.xml")
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(false)
        sarif.required.set(true)
    }
}

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

    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
        "**/*\$Lambda$*.*", // Jacoco can not handle several "$" in class names
        "**/*\$inlined$*.*" // Kotlin specific, Jacoco can not handle several "$" in class names
    )

    val debugTree = fileTree("${project.buildDir}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }

    val mainSrc = "${project.projectDir}/src/main/kotlin"

    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(fileTree(project.buildDir) {
        include("jacoco/testDebugUnitTest.exec")
    })
}