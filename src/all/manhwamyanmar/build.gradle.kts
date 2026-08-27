plugins {
    id("com.android.application") version "8.7.3"
    id("org.jetbrains.kotlin.android") version "2.1.0"
}

android {
    namespace = "eu.kanade.tachiyomi.extension"
    compileSdk = 34

    sourceSets {
        getByName("main") {
            manifest.srcFile("src/main/AndroidManifest.xml")
            java.setSrcDirs(listOf("src"))
            res.setSrcDirs(listOf("res"))
        }
    }

    defaultConfig {
        applicationIdSuffix = "${project.parent?.name}.${project.name}"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.4.1"
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("signingkey.jks")
            storePassword = "mmanhwapass"
            keyAlias = "key0"
            keyPassword = "mmanhwapass"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
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
        aidl = false
        renderScript = false
        resValues = false
        shaders = false
        buildConfig = false
    }

    dependenciesInfo {
        includeInApk = false
    }

    lint {
        checkReleaseBuilds = false
    }
}

dependencies {
    compileOnly("com.github.keiyoushi:extensions-lib:18a8e26be2")
    compileOnly("com.squareup.okhttp3:okhttp:5.4.0")
    compileOnly("org.jsoup:jsoup:1.22.2")
}
