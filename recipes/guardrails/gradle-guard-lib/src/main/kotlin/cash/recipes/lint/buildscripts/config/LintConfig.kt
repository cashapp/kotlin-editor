package cash.recipes.lint.buildscripts.config

import cash.recipes.lint.buildscripts.model.ReportCollection
import cash.recipes.lint.buildscripts.model.Statement
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * Populated from `gradle-lint.yml`. An example file might look like this:
 * ```
 * # Globally-allowed blocks (by name)
 * allowed_blocks:
 *   - "plugins"
 *   - "dependencies"
 *   - "tasks"
 *   - "myCustomExtension"
 *
 * # Globally-allowed statements, matched by prefix.
 * # "tasks." would match "tasks.jar", "tasks.named", etc.
 * allowed_prefixes:
 *   - "val foo ="
 *   - "tasks."
 *
 * # List of paths to ignore. Can be specific files or directories, or a mix.
 * # Directories will include all sub-paths recursively. Trailing `/` is optional.
 * ignored_paths:
 *   - "a/b/build.gradle.kts"
 *   - "a/"
 *   - "a/b"
 *
 * # This section is typically auto-generated via the `baseline` command's functionality in the CLI.
 * # Per-file config, for baseline support
 * baseline:
 *   # One list entry per path (file)
 *   - path: "a/b/build.gradle.kts"
 *     allowed_blocks:
 *       - "myDeprecatedExtension"
 *     allowed_prefixes:
 *       - "val foo ="
 * ```
 *
 * @see [BaselineConfig]
 */
public data class LintConfig(
  private val allowedBlocks: Set<String>? = null,
  private val allowedPrefixes: Set<String>? = null,
  private val ignoredPaths: Set<String>? = null,
  private val baseline: Set<BaselineConfig>? = null,
) {

  init {
    require(baseline?.distinctBy { it.getPath() }?.size == baseline?.size) {
      "Found more than one baseline for the same path."
    }
  }

  public fun getAllowedBlocks(): Set<String> = allowedBlocks?.toSortedSet().orEmpty()

  public fun getAllowedPrefixes(): Set<String> = allowedPrefixes?.toSortedSet().orEmpty()

  public fun getIgnoredPaths(): Set<String> = ignoredPaths?.toSortedSet().orEmpty()

  public fun getBaseline(): Set<BaselineConfig> = baseline?.toSortedSet().orEmpty()

  internal fun merge(other: LintConfig): LintConfig {
    return LintConfig(
      allowedBlocks = (getAllowedBlocks() + other.getAllowedBlocks()).toSortedSet(),
      allowedPrefixes = (getAllowedPrefixes() + other.getAllowedPrefixes()).toSortedSet(),
      ignoredPaths = (getIgnoredPaths() + other.getIgnoredPaths()).toSortedSet(),
      baseline = (getBaseline() + other.getBaseline()).toSortedSet(),
    )
  }

  internal companion object {
    fun parse(yaml: Path): LintConfig {
      return try {
        parse(Files.newInputStream(yaml, StandardOpenOption.READ))
      } catch (e: Exception) {
        // TODO(tsr): use proper logger?
        System.err.println("Error processing gradle guard config file. Expected a YAML file. Was '$yaml'.")
        throw e
      }
    }

    fun parse(yaml: InputStream): LintConfig {
      val mapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule.Builder().build())
      mapper.withCommonConfiguration()
      return mapper.readValue(yaml, LintConfig::class.java)
    }

    fun serialize(config: LintConfig): String {
      val mapper = YAMLMapper()
      mapper.withCommonConfiguration()
      mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY)
      mapper.configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false)
      return mapper.writeValueAsString(config)
    }

    fun of(reports: ReportCollection): LintConfig {
      val baseline: Set<BaselineConfig> = reports.reports.asSequence()
        .mapNotNull { report ->
          val allowedBlocks = report.statements
            .filterIsInstance<Statement.NamedBlock>()
            .map { it.name }
            .toSortedSet()

          val allowedPrefixes = report.statements
            .filterNot { it is Statement.NamedBlock }
            .map { it.text }
            .toSortedSet()

          if (allowedBlocks.isEmpty() && allowedPrefixes.isEmpty()) {
            null
          } else {
            BaselineConfig(
              path = report.buildScript.toString(),
              allowedBlocks = allowedBlocks,
              allowedPrefixes = allowedPrefixes,
            )
          }
        }
        .toSortedSet()

      return LintConfig(baseline = baseline)
    }

    private fun ObjectMapper.withCommonConfiguration(): ObjectMapper {
      return setPropertyNamingStrategy(PropertyNamingStrategies.SnakeCaseStrategy())
    }
  }
}
