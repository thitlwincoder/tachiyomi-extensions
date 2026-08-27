@file:Suppress("UnusedImports")

plugins {
    id("tachiyomi.extension") version "1.3.2"
}

extension {
    name = "Manhwa Myanmar (Adult)"
    pkg = "eu.kanade.tachiyomi.extension.all.manhwamyanmar"
    lang = "my"
    version = 1
    description = "Source for adult.manhwamyanmar.com (Manhwa 18+ Myanmar translations)"
    authors = listOf("thitlwincoder")
    nsfw = true
}

android {
    signingConfigs {
        val releaseSigning = findByName("release") ?: create("release")
        releaseSigning.storeFile = rootProject.file("signingkey.jks")
        releaseSigning.storePassword = "mmanhwapass"
        releaseSigning.keyAlias = "key0"
        releaseSigning.keyPassword = "mmanhwapass"
    }
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
