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
        // Needed for plugins or libraries hosted on JitPack (like StompProtocolAndroid)
        maven { url = uri("https://jitpack.io") }
    }
}

dependencyResolutionManagement {
    // Fail if a module tries to declare its own repository — keeps repos centralized here
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        // Add JitPack for GitHub-hosted libraries
        maven { url = uri("https://jitpack.io") }
    }
}

// Root project name
rootProject.name = "FrontendProject"

// Include your only module
include(":app")
