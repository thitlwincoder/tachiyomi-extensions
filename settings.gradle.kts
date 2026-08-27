pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://www.jitpack.io")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://www.jitpack.io")
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
        var ancestor = moduleDir.parentFile
        while (ancestor != null && ancestor != srcDir) {
            val ancestorPath = ancestor.relativeTo(srcDir).path.replace(File.separatorChar, ':')
            val proj = project(":$ancestorPath")
            if (!proj.projectDir.exists()) proj.projectDir = ancestor
            ancestor = ancestor.parentFile
        }
    }
