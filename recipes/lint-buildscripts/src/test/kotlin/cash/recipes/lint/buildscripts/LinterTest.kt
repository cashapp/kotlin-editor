package cash.recipes.lint.buildscripts

import cash.recipes.lint.buildscripts.utils.BuildScripts
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.io.path.writeText

internal class LinterTest {

  @TempDir
  lateinit var tempDir: Path

  @Test
  fun `can find forbidden statements`() {
    // Given
    val buildScript = tempDir.resolve("build.gradle.kts").also {
      it.writeText(BuildScripts.one)
    }

    val allowList = AllowList.of("plugins", "dependencies")
    val linter = Linter.of(allowList, buildScript, tempDir)

    // When
    val forbiddenStatements = linter.getForbiddenStatements()

    // Then
    assertThat(forbiddenStatements.statements.map { it.text }).containsExactly("tasks", "tasks.jar {")
    assertThat(forbiddenStatements.buildScript.name).isEqualTo("build.gradle.kts")
  }
}
