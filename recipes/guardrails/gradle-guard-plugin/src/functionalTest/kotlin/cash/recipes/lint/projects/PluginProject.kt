package cash.recipes.lint.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.gradle.Plugin

internal class PluginProject : AbstractProject("test-project") {

  fun build(): GradleProject {
    return newGradleProjectBuilder()
      .withSubproject("lib") {
        withBuildScript {
          plugins(Plugin.java)

          withKotlin(
            """
            tasks.register("foo")
            val bar = "1"
            sourceSets {}
            """.trimIndent()
          )
        }
      }
      .write()
  }

  val expectedResult = """
    build.gradle.kts:1 has forbidden block plugins { … }
    lib/build.gradle.kts:1 has forbidden block plugins { … }
    lib/build.gradle.kts:5 has forbidden expression tasks.register("foo") …
    lib/build.gradle.kts:6 has forbidden declaration val bar = "1" …
    lib/build.gradle.kts:7 has forbidden block sourceSets { … }
    settings.gradle.kts:1 has forbidden block pluginManagement { … }
    settings.gradle.kts:10 has forbidden block dependencyResolutionManagement { … }
    settings.gradle.kts:18 has forbidden assignment rootProject.name = "test-project" …
    settings.gradle.kts:20 has forbidden expression include(":lib") …
  """.trimIndent()

  val expectedBaseline = """
    |baseline:
    |- path: "build.gradle.kts"
    |  allowed_blocks:
    |  - "plugins"
    |- path: "lib/build.gradle.kts"
    |  allowed_blocks:
    |  - "plugins"
    |  - "sourceSets"
    |  allowed_prefixes:
    |  - "tasks.register(\"foo\")"
    |  - "val bar = \"1\""
    |- path: "settings.gradle.kts"
    |  allowed_blocks:
    |  - "dependencyResolutionManagement"
    |  - "pluginManagement"
    |  allowed_prefixes:
    |  - "include(\":lib\")"
    |  - "rootProject.name = \"test-project\""
    |
  """.trimMargin()
}
