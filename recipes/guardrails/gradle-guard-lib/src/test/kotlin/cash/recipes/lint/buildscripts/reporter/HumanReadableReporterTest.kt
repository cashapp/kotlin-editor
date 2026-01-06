package cash.recipes.lint.buildscripts.reporter

import cash.recipes.lint.buildscripts.Linter
import cash.recipes.lint.buildscripts.config.AllowList
import cash.recipes.lint.buildscripts.utils.BuildScripts
import cash.recipes.lint.buildscripts.utils.TestLogger
import cash.recipes.lint.buildscripts.utils.withContent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

internal class HumanReadableReporterTest {

  @TempDir
  lateinit var tempDir: Path

  @Test
  fun `no report when no violations`() {
    // Given
    tempDir.resolve("build.gradle.kts").withContent(BuildScripts.noViolations)
    val allowList = AllowList.ofNamedBlocks("plugins", "dependencies")
    val linter = Linter.of(allowList, listOf(tempDir))
    val logger = TestLogger()

    // When
    val reporter = HumanReadableReporter.of(linter, logger)
    reporter.printReport()

    // Then
    val messages = logger.getMessages()
    assertThat(messages).size().isEqualTo(1)
    assertThat(messages.single()).isEqualTo("None of the build scripts contain forbidden statements.")
  }

  @Test
  fun `can print human-readable report for a single file`() {
    // Given
    val buildScript = tempDir.resolve("build.gradle.kts").withContent(BuildScripts.hasViolations1)
    val allowList = AllowList.ofNamedBlocks("plugins", "dependencies")
    val linter = Linter.of(allowList, listOf(buildScript), tempDir)
    val logger = TestLogger()

    // When
    val reporter = HumanReadableReporter.of(linter, logger)
    reporter.printReport()

    // Then
    assertThat(logger.getMessages()).containsExactly(
      """
        |The build script 'build.gradle.kts' contains 2 forbidden statements:
        |
        |1: tasks { … (named block)
        |   Start: (Line: 24, Column: 0)
        |   End:   (Line: 28, Column: 0)
        |
        |2: tasks.jar { … (expression)
        |   Start: (Line: 30, Column: 0)
        |   End:   (Line: 32, Column: 0)
        |
      """.trimMargin()
    )
  }

  @Test
  fun `can print human-readable report for a collection of files`() {
    // Given
    tempDir.resolve("build.gradle.kts").withContent(BuildScripts.noViolations)
    tempDir.resolve("a/build.gradle.kts").withContent(BuildScripts.hasViolations1)
    tempDir.resolve("b/c/build.gradle.kts").withContent(BuildScripts.hasViolations2)
    val allowList = AllowList.ofNamedBlocks("plugins", "dependencies")
    val linter = Linter.of(allowList, listOf(tempDir))
    val logger = TestLogger()

    // When
    val reporter = HumanReadableReporter.of(linter, logger)
    reporter.printReport()

    // Then
    val messages = logger.getMessages()
    assertThat(messages).size().isEqualTo(1)

    val message = messages.single()
    val firstLine = message.substringBefore(System.lineSeparator())
    assertThat(firstLine).startsWith("Analysis complete.")

    val messageWithoutFirstLine = message.substringAfter(System.lineSeparator())
    assertThat(messageWithoutFirstLine).isEqualTo(
      """
        |- 1 without any violations.
        |- 2 with violations.
        |
        |The build script 'a/build.gradle.kts' contains 2 forbidden statements:
        |
        |1: tasks { … (named block)
        |   Start: (Line: 24, Column: 0)
        |   End:   (Line: 28, Column: 0)
        |
        |2: tasks.jar { … (expression)
        |   Start: (Line: 30, Column: 0)
        |   End:   (Line: 32, Column: 0)
        |
        |The build script 'b/c/build.gradle.kts' contains 1 forbidden statement:
        |
        |1: val foo = 1 … (declaration)
        |   Start: (Line: 6, Column: 0)
        |   End:   (Line: 6, Column: 10)
        |
      """.trimMargin()
    )
  }
}
