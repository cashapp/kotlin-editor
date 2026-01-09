package cash.recipes.lint.buildscripts.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class LintConfigTest {

  @Test
  fun `test`() {
    // Given
    val yaml = """
      allowed_blocks:
        - "plugins"
        - "dependencies"
        - "tasks"
        - "myCustomExtension"

      allowed_prefixes:
        - "val foo ="
        - "tasks."

      ignored_paths:
        - "a/b/build.gradle.kts"
        - "a/"
        - "a/b"
    """.trimIndent()

    // When
    val config = LintConfig.parse(yaml)

    // Then
    assertThat(config.allowedBlocks()).containsExactly("plugins", "dependencies", "tasks", "myCustomExtension")
    assertThat(config.allowedPrefixes()).containsExactly("val foo =", "tasks.")
    assertThat(config.ignoredPaths()).containsExactly("a/b/build.gradle.kts", "a/", "a/b")
  }
}