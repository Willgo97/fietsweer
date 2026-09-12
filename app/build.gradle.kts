import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "nl.fietsweer.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "nl.fietsweer.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.2"
        resourceConfigurations += listOf("en", "nl")
    }

    // Release signing comes from ~/.gradle/gradle.properties (or the matching
    // environment variables), never from the repository. Without them the build
    // still works and simply falls back to the debug key, so a fresh clone can
    // be built by anyone.
    val releaseStore = (findProperty("FIETSWEER_STORE_FILE") as String?)
        ?: System.getenv("FIETSWEER_STORE_FILE")
    val releaseSigning = if (releaseStore != null && file(releaseStore).exists()) {
        signingConfigs.create("release") {
            storeFile = file(releaseStore)
            storePassword = (findProperty("FIETSWEER_STORE_PASSWORD") as String?)
                ?: System.getenv("FIETSWEER_STORE_PASSWORD")
            keyAlias = (findProperty("FIETSWEER_KEY_ALIAS") as String?)
                ?: System.getenv("FIETSWEER_KEY_ALIAS")
            keyPassword = (findProperty("FIETSWEER_KEY_PASSWORD") as String?)
                ?: System.getenv("FIETSWEER_KEY_PASSWORD")
        }
    } else {
        logger.lifecycle("No release keystore configured; signing with the debug key.")
        null
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = releaseSigning ?: signingConfigs.getByName("debug")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "META-INF/*.version")
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")

    implementation(platform("androidx.compose:compose-bom:2025.09.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.compose.animation:animation")

    implementation("androidx.work:work-runtime-ktx:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
