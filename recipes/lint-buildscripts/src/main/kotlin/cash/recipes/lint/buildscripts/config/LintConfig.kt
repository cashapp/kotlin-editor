package cash.recipes.lint.buildscripts.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule

/**
 * Populated from `gradle-lint.yml`. An example file might look like this:
 * ```
 * # Globally-allowed blocks (by name)
 * allowed-blocks:
 *   - "plugins"
 *   - "dependencies"
 *   - "tasks"
 *   - "myCustomExtension"
 *
 * # Globally-allowed statements, matched by prefix.
 * # "tasks." would match "tasks.jar", "tasks.named", etc.
 * allowed-prefixes:
 *   - "val foo ="
 *   - "tasks."
 *
 * # List of paths to ignore. Can be specific files or directories, or a mix.
 * # Directories will include all sub-paths recursively. Trailing `/` is optional.
 * ignored-paths:
 *   - "a/b/build.gradle.kts"
 *   - "a/"
 *   - "a/b"
 * ```
 */
internal data class LintConfig(
  private val allowed_blocks: List<String>?,
  private val allowed_prefixes: List<String>?,
  private val ignored_paths: List<String>?,
) {

  fun allowedBlocks(): List<String> = allowed_blocks.orEmpty()

  fun allowedPrefixes(): List<String> = allowed_prefixes.orEmpty()

  fun ignoredPaths(): List<String> = ignored_paths.orEmpty()

  internal companion object {
    fun parse(yaml: String): LintConfig {
      val objectMapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule.Builder().build())
      return objectMapper.readValue(yaml, LintConfig::class.java)
    }

    fun serialize(config: LintConfig): String {
      val yamlMapper = YAMLMapper()
      yamlMapper.setDefaultPropertyInclusion(JsonInclude.Include.ALWAYS)
      yamlMapper.configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false)
      return yamlMapper.writeValueAsString(config)
    }
  }
}
