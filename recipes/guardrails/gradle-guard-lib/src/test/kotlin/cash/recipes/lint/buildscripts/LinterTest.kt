package cash.recipes.lint.buildscripts

import cash.recipes.lint.buildscripts.config.AllowList
import cash.recipes.lint.buildscripts.config.BaselineConfig
import cash.recipes.lint.buildscripts.config.LintConfig
import cash.recipes.lint.buildscripts.model.Position
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

    val allowList = AllowList.ofNamedBlocks("plugins", "dependencies")
    val linter = Linter.of(allowList, listOf(buildScript), tempDir)

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

    val allowList = AllowList.ofNamedBlocks("plugins", "dependencies")
    val linter = Linter.of(allowList, listOf(tempDir))

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

      override fun test(path: Path): Boolean = path.startsWith(Path.of("a"))
    }
    val allowList = AllowList.of(spec)
    val linter = Linter.of(allowList, listOf(tempDir))

    // When
    val reports = linter.getReports()

    // Then
    assertThat(reports.reports).size().isEqualTo(2)

    with(reports.reports.single { it.buildScript.toString() == "build.gradle.kts" }) {
      assertThat(buildScript.toString()).isEqualTo("build.gradle.kts")
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

      override fun test(path: Path): Boolean = path.startsWith(Path.of("a"))
    }
    val allowList = AllowList.of(spec)
    val linter = Linter.of(allowList, listOf(tempDir))

    // When
    val reports = linter.getReports()

    // Then
    assertThat(reports.reports).size().isEqualTo(1)

    with(reports.reports.single { it.buildScript.toString() == "build.gradle.kts" }) {
      assertThat(buildScript.toString()).isEqualTo("build.gradle.kts")
      assertThat(statements.map { it.text }).containsExactly("plugins")
    }
  }

  @Test
  fun `can lint with yaml config file`() {
    tempDir.resolve("build.gradle.kts").withContent(BuildScripts.noViolations)
    tempDir.resolve("a/b/build.gradle.kts").withContent(BuildScripts.hasViolations1)
    tempDir.resolve("a/c/build.gradle.kts").withContent(BuildScripts.hasViolations2)
    tempDir.resolve("d/build.gradle.kts").withContent(BuildScripts.hasViolations3)

    val yaml = """
      allowed_blocks:
        - "plugins"
        - "dependencies"
        - "tasks"

      allowed_prefixes:
        - "tasks."

      ignored_paths:
        - "d"
    """.trimIndent()
    val configFile = tempDir.resolve("kotlin-dsl-config.yml").withContent(yaml)

    val allowList = AllowList.of(configFile)
    val linter = Linter.of(allowList, listOf(tempDir))

    // When
    val reports = linter.getReports()

    // Then
    // There are four scripts but one has been ignored.
    assertThat(reports.reports).size().isEqualTo(3)

    with(reports.reports.single { it.buildScript.toString() == "build.gradle.kts" }) {
      assertThat(buildScript.toString()).isEqualTo("build.gradle.kts")
      assertThat(statements).isEmpty()
    }
    with(reports.reports.single { it.buildScript.toString() == "a/b/build.gradle.kts" }) {
      assertThat(buildScript.toString()).isEqualTo("a/b/build.gradle.kts")
      assertThat(statements).isEmpty()
    }
    with(reports.reports.single { it.buildScript.toString() == "a/c/build.gradle.kts" }) {
      assertThat(buildScript.toString()).isEqualTo("a/c/build.gradle.kts")
      assertThat(statements).containsExactly(
        Statement.Declaration(firstLine = "val foo = 1", start = Position(6, 0), stop = Position(6, 10))
      )
    }
  }

  @Test
  fun `can create a baseline`() {
    // Given
    tempDir.resolve("build.gradle.kts").withContent(BuildScripts.noViolations)
    tempDir.resolve("a/b/build.gradle.kts").withContent(BuildScripts.hasViolations1)
    tempDir.resolve("a/c/build.gradle.kts").withContent(BuildScripts.hasViolations2)
    tempDir.resolve("d/build.gradle.kts").withContent(BuildScripts.hasViolations3)

    val yaml = """
      allowed_blocks:
        - "plugins"
        - "dependencies"
        - "tasks"

      allowed_prefixes:
        - "tasks."

      ignored_paths:
        - "d"
    """.trimIndent()
    val configFile = tempDir.resolve("kotlin-dsl-config.yml").withContent(yaml)

    val allowList = AllowList.of(configFile)
    val linter = Linter.of(allowList, listOf(tempDir))

    // When
    val dest = tempDir.resolve("baseline.yml")
    linter.writeBaseline(dest)

    // Then the new config should be a merging of the original config + the new baseline
    assertThat(dest).content().isEqualTo(
      """
        |allowed_blocks:
        |- "dependencies"
        |- "plugins"
        |- "tasks"
        |allowed_prefixes:
        |- "tasks."
        |ignored_paths:
        |- "d"
        |baseline:
        |- path: "a/c/build.gradle.kts"
        |  allowed_prefixes:
        |  - "val foo = 1"
        |
      """.trimMargin()
    )

    // and it can be parsed (validating round-trip)
    assertThat(LintConfig.parse(dest)).isEqualTo(
      LintConfig(
        allowedBlocks = setOf("dependencies", "plugins", "tasks"),
        allowedPrefixes = setOf("tasks."),
        ignoredPaths = setOf("d"),
        baseline = setOf(
          BaselineConfig(
            path = "a/c/build.gradle.kts",
            allowedPrefixes = setOf("val foo = 1"),
          )
        )
      )
    )
  }
}
