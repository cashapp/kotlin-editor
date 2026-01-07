package cash.recipes.lint.buildscripts

import cash.recipes.lint.buildscripts.utils.BuildScripts
import cash.recipes.lint.buildscripts.utils.TestLogger
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

class ConsoleReporterTest {

  @TempDir
  lateinit var tempDir: Path

  @Test
  fun `can print human-readable report`() {
    // Given
    val buildScript = tempDir.resolve("build.gradle.kts").also {
      it.writeText(BuildScripts.one)
    }
    val allowList = AllowList.of("plugins", "dependencies")
    val linter = Linter.of(allowList, buildScript, tempDir)
    val logger = TestLogger()

    // When
    val reporter = ConsoleReporter.of(linter, logger)
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
}
