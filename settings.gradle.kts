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
  id("com.gradle.develocity") version "4.3"
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
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
