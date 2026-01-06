package cash.recipes.lint.buildscripts.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class LintConfigTest {

  @Test
  fun `can parse a yml file into a LintConfig`() {
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
    val config = LintConfig.parse(yaml.byteInputStream())

    // Then
    assertThat(config.getAllowedBlocks()).containsExactly("dependencies", "myCustomExtension", "plugins", "tasks")
    assertThat(config.getAllowedPrefixes()).containsExactly("tasks.", "val foo =")
    assertThat(config.getIgnoredPaths()).containsExactly("a/", "a/b", "a/b/build.gradle.kts")
  }

  @Test
  fun `can parse a baseline`() {
    // Given
    val yaml = """
      baseline:
        - path: "a/b/build.gradle.kts"
          allowed_blocks:
            - "myDeprecatedExtension"
          allowed_prefixes:
            - "val foo ="
    """.trimIndent()

    // When
    val config = LintConfig.parse(yaml.byteInputStream())

    // Then
    with(config.getBaseline()) {
      assertThat(this).size().isEqualTo(1)

      with(first()) {
        assertThat(getPath().toString()).isEqualTo("a/b/build.gradle.kts")
        assertThat(getAllowedBlocks()).containsExactly("myDeprecatedExtension")
        assertThat(getAllowedPrefixes()).containsExactly("val foo =")
      }
    }
  }
}
