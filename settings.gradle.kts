rootProject.name = "kotlin-editor"

pluginManagement {
  includeBuild("build-logic")

  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins {
  id("com.gradle.develocity") version "3.17.1"
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    mavenCentral()
  }
}

// TODO(tsr): build scans
//  1. note this is publishing to the public scans server
//  2. make it possible to opt-in to build scan publication
develocity {
  buildScan {
    termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
    termsOfUseAgree = "yes"
    publishing.onlyIf { false }
  }
}

include(":core")
include(":grammar")
include(":recipes:plugins")
include(":recipes:repos")
