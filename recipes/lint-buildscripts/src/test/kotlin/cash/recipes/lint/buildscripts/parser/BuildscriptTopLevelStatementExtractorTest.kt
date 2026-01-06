package cash.recipes.lint.buildscripts.parser

import cash.recipes.lint.buildscripts.model.Position
import cash.recipes.lint.buildscripts.model.Statement
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class BuildscriptTopLevelStatementExtractorTest {

  @Test
  fun `can simplify dependency declarations`() {
    // Given
    val buildScript = """
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

    // When
    val linter = BuildscriptTopLevelStatementExtractor.of(buildScript)
    val namedBlocks = linter.getStatements()

    // Then
    Assertions.assertThat(namedBlocks).containsExactlyInAnyOrder(
      Statement.NamedBlock("plugins", Position(1, 0), Position(4, 0)),
      Statement.NamedBlock("dependencies", Position(6, 0), Position(22, 0)),
      Statement.NamedBlock("tasks", Position(24, 0), Position(28, 0)),
      Statement.Expression("tasks.jar {",
        Position(line = 30, positionInLine = 0), stop = Position(line = 32, positionInLine = 0)
      ),
    )
  }
}