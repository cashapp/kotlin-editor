package cash.recipes.lint.buildscripts

import cash.recipes.lint.buildscripts.config.AllowList
import cash.recipes.lint.buildscripts.config.LintConfig
import cash.recipes.lint.buildscripts.model.Report
import cash.recipes.lint.buildscripts.model.ReportCollection
import cash.recipes.lint.buildscripts.parser.BuildscriptTopLevelStatementExtractor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.walk
import kotlin.io.path.writeText

/**
 * Given an [allowList] and set of [paths] (files) to inspect, can either:
 * 1. Run a check ([getReports]) on those files; or
 * 2. Write a baseline ([writeBaseline]) that combines the provided [allowList] with the [reports][getReports] to create
 *    a new config file that would result in an empty report if used as the config file.
 */
public class Linter private constructor(
  private val allowList: AllowList,
  private val paths: List<Path>,
  private val root: Path? = null,
) {

  private val _reports by lazy(LazyThreadSafetyMode.NONE) {
    val statements = getBuildScripts()
      .filterNot { (_, relativePath) -> allowList.isIgnoredBuildScript(relativePath) }
      .map { (buildScript, relativePath) ->
        val extractor = BuildscriptTopLevelStatementExtractor.of(buildScript)

        Report(
          relativePath,
          allowList.forbiddenStatements(relativePath, extractor.getStatements()),
        )
      }
      .toList()

    ReportCollection(statements)
  }

  /** Returns a list of reports, one for each build script parsed. */
  public fun getReports(): ReportCollection = _reports

  /**
   * Generates a [LintConfig] which, if used while linting, would result in finding no violations. Said another way,
   * all violations discovered with the prior config are allow-listed with the config returned by this method.
   */
  public fun generateBaselineConfig(): LintConfig {
    return listOf(LintConfig.of(_reports))
      .plus(allowList.getLintConfigs())
      .reduce(LintConfig::merge)
  }

  /**
   * Generates the string form of a [LintConfig] which, if used while linting, would result in finding no violations.
   * Said another way, all violations discovered with the prior config are allow-listed with the config returned by this
   * method.
   */
  public fun generateBaseline(): String {
    return LintConfig.serialize(generateBaselineConfig())
  }

  /**
   * Writes out the [LintConfig] returned by [generateBaseline] to [dest], which could then be used as the new lint
   * config.
   */
  public fun writeBaseline(dest: Path) {
    // TODO(tsr) use a logger instead of System.err directly
    if (dest.exists() && dest.fileSize() != 0L) {
      // Warn users if we're clobbering a non-empty file
      val copy = Files.createTempFile("baseline", ".yml")
      System.err.println("'$dest' exists. This operation will overwrite it. Copying current file to '$copy'.")
      Files.copy(dest, copy, StandardCopyOption.REPLACE_EXISTING)
    }

    dest.writeText(generateBaseline())
  }

  /** True if there are any violations in any of the parsed build scripts. */
  public fun hasErrors(): Boolean = _reports.hasErrors()

  /** True if there are no violations in any of the parsed build scripts. */
  public fun hasNoErrors(): Boolean = !hasErrors()

  /**
   * Returns a sequence of Kotlin DSL files (`.gradle.kts`, which includes build and settings scripts, etc.) paired with
   * their [relativePath]. This sequence is an expansion of [paths] that includes every explicitly included regular
   * file, along with every regular file found by recursively walking each path that is a directory.
   */
  private fun getBuildScripts(): Sequence<Pair<Path, Path>> {
    return paths.asSequence()
      .flatMap { path ->
        require(path.exists()) { "$path does not exist!" }

        if (path.isDirectory()) {
          path.walk()
            .filter { p -> p.isRegularFile() }
            .filter { p -> p.exists() }
            .filter { p -> p.isKotlinDsl() }
            .map { p -> p to relativePath(p, path) }
        } else if (path.isRegularFile()) {
          require(path.isKotlinDsl()) { "Expected a Kotlin DSL script. Was '$path'." }
          sequenceOf(path to relativePath(path, path))
        } else {
          error("Expected either a regular file or a directory. Was '$path'.")
        }
      }
  }

  /**
   * This method avoids the easy footgun of [Path.endsWith(String)][Path.endsWith], which if called like
   * `path.endsWith("gradle.kts")` would always be false, because the _last segment of a path_ is the full file name,
   * and we want to include _all_ Kotlin DSL scripts as well as build scripts that might have their names configured to
   * be different from the standard `build.gradle.kts`.
   */
  private fun Path.isKotlinDsl(): Boolean = name.endsWith("gradle.kts")

  /**
   * We want the path to the build script relative to the root (`path`). In the case where `path` IS the build script,
   * then the relative path is just the last part of the `path`.
   */
  private fun relativePath(buildScript: Path, base: Path): Path {
    val base = root ?: base
    return if (base == buildScript) buildScript.last() else base.relativize(buildScript)
  }

  public companion object {
    /**
     * Returns a [Linter], which reports problems found at [paths] after filtering out anything matching the
     * [allowList]. [paths] may be either a single build script (`.gradle.kts` file), a directory, or a combination of
     * the two. Any directories will be walked recursively and every build script within will be linted.
     *
     * If [root] (optional) is included, all [paths] will be relativized to that root. This is helpful for readability
     * and necessary for baseline functionality when multiple paths are passed.
     */
    public fun of(allowList: AllowList, paths: List<Path>, root: Path? = null): Linter {
      return Linter(allowList = allowList, paths = paths, root = root)
    }
  }
}
