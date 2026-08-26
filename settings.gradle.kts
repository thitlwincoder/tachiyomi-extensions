pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.mihon.app")
        maven("https://maven.tachiyomi.org")
    }
}

rootProject.name = "tachiyomi-extensions"

val srcDir = file("src")
srcDir.walkTopDown()
    .filter { it.isFile && it.name == "build.gradle.kts" }
    .forEach { buildFile ->
        val moduleDir = buildFile.parentFile
        val modulePath = moduleDir.relativeTo(srcDir).path.replace(File.separatorChar, ':')
        include(":$modulePath")
        project(":$modulePath").projectDir = moduleDir
    }
