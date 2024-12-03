rootProject.name = "kotlin-editor"

pluginManagement {
  includeBuild("build-logic")

  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins {
  // Keep this version in sync with version catalog
  id("com.gradle.develocity") version "3.17.5"
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
  id("cash.settings")
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    mavenCentral()
  }
}

include(":core")
include(":grammar")
include(":recipes:dependencies")
include(":recipes:plugins")
include(":recipes:repos")
