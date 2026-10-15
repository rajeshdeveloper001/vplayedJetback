pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Local Maven repository produced by vplayedKMP's publish task
        maven {
            name = "KmpLocalMaven"
            val localRepoDir = file("../vplayedKMP/repo/localMaven").takeIf { it.exists() }
                ?: file("../kmp/repo/localMaven").takeIf { it.exists() }
                ?: file("../vplayed-kmp/repo/localMaven")
            url = uri(localRepoDir)
        }
    }
}

rootProject.name = "Vplayed3.0"
include(":app")

// Shared business logic (Kotlin Multiplatform library `:shared`).
// Resolution mode:
//   1. Default: Consumes published local AAR artifact from KmpLocalMaven repository.
//   2. Composite build: Enabled when -Pvplayed.useComposite=true is passed.
val useComposite = providers.gradleProperty("vplayed.useComposite").orNull?.toBoolean() ?: false
if (useComposite) {
    val kmpDir: String = run {
        val explicit = providers.gradleProperty("vplayed.kmpDir").orNull
        if (explicit != null) {
            return@run explicit
        }
        val candidates = listOf("../kmp", "../vplayedKMP", "../vplayed-kmp")
        candidates.firstOrNull { dir ->
            file(dir).let { it.isDirectory && (it.resolve("settings.gradle.kts").exists() || it.resolve("settings.gradle").exists()) }
        } ?: error(
            "KMP repository not found for composite build. Checked: ${candidates.joinToString()}.\n" +
            "Either set the Gradle property vplayed.kmpDir or build the AAR via ./refresh_shared_aar.sh"
        )
    }
    includeBuild(kmpDir) {
        dependencySubstitution {
            substitute(module("com.vplayed:shared")).using(project(":shared"))
        }
    }
}
 