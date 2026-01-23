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

internal class MachineReadableReporterTest {

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
    val reporter = MachineReadableReporter.of(linter, logger)
    reporter.printReport()

    // Then
    val messages = logger.getMessages()
    assertThat(messages).size().isEqualTo(1)
    assertThat(messages.single()).isBlank()
  }

  @Test
  fun `can print machine-readable report for a single file`() {
    // Given
    val buildScript = tempDir.resolve("build.gradle.kts").withContent(BuildScripts.hasViolations1)
    val allowList = AllowList.ofNamedBlocks("plugins", "dependencies")
    val linter = Linter.of(allowList, listOf(buildScript), tempDir)
    val logger = TestLogger()

    // When
    val reporter = MachineReadableReporter.of(linter, logger)
    reporter.printReport()

    // Then
    assertThat(logger.getMessages()).containsExactly(
      """
        |build.gradle.kts:24 has forbidden block tasks { … }
        |build.gradle.kts:30 has forbidden expression tasks.jar { …
        |
      """.trimMargin()
    )
  }

  @Test
  fun `can print machine-readable report for a collection of files`() {
    // Given
    tempDir.resolve("build.gradle.kts").withContent(BuildScripts.noViolations)
    tempDir.resolve("a/build.gradle.kts").withContent(BuildScripts.hasViolations1)
    tempDir.resolve("b/c/build.gradle.kts").withContent(BuildScripts.hasViolations2)
    val allowList = AllowList.ofNamedBlocks("plugins", "dependencies")
    val linter = Linter.of(allowList, listOf(tempDir))
    val logger = TestLogger()

    // When
    val reporter = MachineReadableReporter.of(linter, logger)
    reporter.printReport()

    // Then
    val messages = logger.getMessages()
    assertThat(messages).size().isEqualTo(1)
    assertThat(messages.single()).isEqualTo(
      """
        |a/build.gradle.kts:24 has forbidden block tasks { … }
        |a/build.gradle.kts:30 has forbidden expression tasks.jar { …
        |b/c/build.gradle.kts:6 has forbidden declaration val foo = 1 …
        |
      """.trimMargin()
    )
  }
}
