package cash.recipes.lint.buildscripts

import cash.recipes.lint.buildscripts.model.Statement
import cash.recipes.lint.buildscripts.utils.BuildScripts
import cash.recipes.lint.buildscripts.utils.withContent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.name

internal class LinterTest {

  @TempDir
  lateinit var tempDir: Path

  @Test
  fun `can find forbidden statements in a single file`() {
    // Given
    val buildScript = tempDir.resolve("build.gradle.kts").withContent(BuildScripts.hasViolations1)

    val allowList = AllowList.of("plugins", "dependencies")
    val linter = Linter.of(allowList, buildScript)

    // When
    val forbiddenStatements = linter.getReports().reports.single()

    // Then
    assertThat(forbiddenStatements.statements.map { it.text }).containsExactly("tasks", "tasks.jar {")
    assertThat(forbiddenStatements.buildScript.name).isEqualTo("build.gradle.kts")
  }

  @Test
  fun `can find forbidden statements in a directory hierarchy`() {
    // Given
    tempDir.resolve("build.gradle.kts").withContent(BuildScripts.noViolations)
    tempDir.resolve("a/build.gradle.kts").withContent(BuildScripts.hasViolations1)
    tempDir.resolve("b/c/build.gradle.kts").withContent(BuildScripts.hasViolations2)

    val allowList = AllowList.of("plugins", "dependencies")
    val linter = Linter.of(allowList, tempDir)

    // When
    val reports = linter.getReports()

    // Then
    assertThat(reports.reports).size().isEqualTo(3)

    with(reports.reports.single { it.buildScript.toString() == "build.gradle.kts" }) {
      assertThat(buildScript.toString()).isEqualTo("build.gradle.kts")
      assertThat(statements).isEmpty()
    }
    with(reports.reports.single { it.buildScript.toString() == "a/build.gradle.kts" }) {
      assertThat(buildScript.toString()).isEqualTo("a/build.gradle.kts")
      assertThat(statements.map { it.text }).containsExactly("tasks", "tasks.jar {")
    }
    with(reports.reports.single { it.buildScript.toString() == "b/c/build.gradle.kts" }) {
      assertThat(buildScript.toString()).isEqualTo("b/c/build.gradle.kts")
      assertThat(statements.map { it.text }).containsExactly("val foo = 1")
    }
  }

  @Test
  fun `can lint with complex allow lists`() {
    // Given
    tempDir.resolve("build.gradle.kts").withContent(BuildScripts.noViolations)
    tempDir.resolve("a/build.gradle.kts").withContent(BuildScripts.hasViolations1)
    tempDir.resolve("b/c/build.gradle.kts").withContent(BuildScripts.hasViolations2)

    val spec = object : AllowList.Spec {
      private val allowedNames = listOf("plugins", "dependencies")

      override fun test(buildScript: Path, stmt: Statement): Boolean {
        // the "a/build.gradle.kts" script is fully allow-listed.
        return buildScript.parent?.name == "a"
            // Any statement anywhere matching this spec is also allow-listed.
            || stmt.text in allowedNames
      }
    }
    val allowList = AllowList.of(spec)
    val linter = Linter.of(allowList, tempDir)

    // When
    val reports = linter.getReports()

    // Then
    assertThat(reports.reports).size().isEqualTo(3)

    with(reports.reports.single { it.buildScript.toString() == "build.gradle.kts" }) {
      assertThat(buildScript.toString()).isEqualTo("build.gradle.kts")
      assertThat(statements).isEmpty()
    }
    with(reports.reports.single { it.buildScript.toString() == "a/build.gradle.kts" }) {
      assertThat(buildScript.toString()).isEqualTo("a/build.gradle.kts")
      assertThat(statements).isEmpty()
    }
    with(reports.reports.single { it.buildScript.toString() == "b/c/build.gradle.kts" }) {
      assertThat(buildScript.toString()).isEqualTo("b/c/build.gradle.kts")
      assertThat(statements.map { it.text }).containsExactly("val foo = 1")
    }
  }

  @Test
  fun `can allow-list a directory tree`() {
    // Given
    tempDir.resolve("build.gradle.kts").withContent(BuildScripts.noViolations)
    tempDir.resolve("a/b/build.gradle.kts").withContent(BuildScripts.hasViolations1)
    tempDir.resolve("a/c/build.gradle.kts").withContent(BuildScripts.hasViolations2)

    val spec = object : AllowList.Spec {
      override fun test(buildScript: Path, stmt: Statement): Boolean {
        // the "a/build.gradle.kts" script is fully allow-listed.
        return buildScript.startsWith(Path.of("a"))
      }
    }
    val allowList = AllowList.of(spec)
    val linter = Linter.of(allowList, tempDir)

    // When
    val reports = linter.getReports()

    // Then
    assertThat(reports.reports).size().isEqualTo(3)

    with(reports.reports.single { it.buildScript.toString() == "build.gradle.kts" }) {
      assertThat(buildScript.toString()).isEqualTo("build.gradle.kts")
      assertThat(statements.map { it.text }).containsExactly("plugins")
    }
    with(reports.reports.single { it.buildScript.toString() == "a/b/build.gradle.kts" }) {
      assertThat(buildScript.toString()).isEqualTo("a/b/build.gradle.kts")
      assertThat(statements).isEmpty()
    }
    with(reports.reports.single { it.buildScript.toString() == "a/c/build.gradle.kts" }) {
      assertThat(buildScript.toString()).isEqualTo("a/c/build.gradle.kts")
      assertThat(statements).isEmpty()
    }
  }
}
