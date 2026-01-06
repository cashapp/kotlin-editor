package cash.recipes.lint.buildscripts

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

internal class LinterTest {

  @TempDir
  lateinit var tempDir: Path

  @Test
  fun `can find forbidden statements`() {
    // Given
    val content = """
      plugins {
        id("foo")
        alias(libs.plugins.bar)
      }
      
      dependencies {
        constraints {
          implementation("com.foo:bar") {
            version {
              require("1")
            }
          }
        }
      
        implementation(libs.foo)
        api("com.foo:bar:1.0")
        runtimeOnly(group = "foo", name = "bar", version = "2.0")
        compileOnly(group = "foo", name = "bar", version = libs.versions.bar.get()) {
          isTransitive = false
        }
        devImplementation(group = "io.netty", name = "netty-transport-native-kqueue", classifier = "osx-x86_64")
      }
      
      tasks {
        jar {
          archiveClassifier.set("unshaded")
        }
      }
      
      tasks.jar {
        archiveClassifier.set("unshaded")
      }
    """.trimIndent()

    val buildScript = tempDir.resolve("build.gradle.kts").also {
      it.writeText(content)
    }

    val allowList = AllowList.of("plugins", "dependencies")
    val linter = Linter.of(buildScript, allowList)

    // When
    val forbiddenStatements = linter.getForbiddenStatements()

    // Then
    assertThat(forbiddenStatements.map { it.text }).containsExactly("tasks", "tasks.jar {")
  }
}
