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
    }
}

rootProject.name = "Vplayed3.0"
include(":app")

// Shared business logic (Kotlin Multiplatform library `:shared`), built from source from the kmp
// repository checked out next to this one (../kmp). See docs/architecture/shared-module.md.
includeBuild("../kmp") {
    dependencySubstitution {
        substitute(module("com.vplayed:shared")).using(project(":shared"))
    }
}
 