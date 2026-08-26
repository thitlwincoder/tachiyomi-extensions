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
    authors = listOf("")
    nsfw = true
}
