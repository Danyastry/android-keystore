pluginManagement {
    includeBuild("build-logic")
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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "android-keystore"

include(":app")
include(":core:keystore")
include(":core:ui")

include(":feature:aesgcm")
include(":feature:rsaoaep")
include(":feature:ecdsa")
include(":feature:hmac")
include(":feature:keyinfo")
include(":feature:strongbox")
include(":feature:attestation")
include(":feature:biometric")
include(":feature:authtimeout")
include(":feature:unlockeddevice")
include(":feature:ecdh")
include(":feature:keyimport")
include(":feature:keyconstraints")
