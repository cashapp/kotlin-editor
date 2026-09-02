import org.gradle.plugin.compatibility.compatibility

plugins {
  id("cash.plugin")
}

kotlinEditor {
  group("app.cash.gradle-guard")
  version(providers.gradleProperty("cashapp.gradle-guard-version").orElse("0.4.1").get())
}

gradlePlugin {
  plugins {
    register("plugin") {
      id = "app.cash.gradle-guard"
      implementationClass = "cash.recipes.lint.GradleGuardPlugin"

      displayName = "Gradle Guard"
      description = "Checks Gradle Kotlin DSL scripts against allow-list of prescribed elements"
      tags = listOf("linting")

      compatibility {
        features {
          configurationCache = true
        }
      }
    }
  }

  website = "https://github.com/cashapp/kotlin-editor/tree/main/recipes/guardrails"
  vcsUrl = "https://github.com/cashapp/kotlin-editor"
}

// Populates the version at build time into a file that can be read by Java resources at runtime
tasks.processResources {
  inputs.property("version", version)

  from("app-cash-gradle-guard-version.txt")
  expand(mapOf("version" to version))
}

gradleTestKitSupport {
  withClasspaths("functionalTestRuntimeClasspath")
}

dependencies {
  functionalTestRuntimeOnly(project(":recipes:guardrails:gradle-guard"))
}
