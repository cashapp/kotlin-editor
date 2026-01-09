package cash.recipes.lint.buildscripts

import cash.recipes.lint.buildscripts.model.Report
import cash.recipes.lint.buildscripts.model.ReportCollection
import cash.recipes.lint.buildscripts.parser.BuildscriptTopLevelStatementExtractor
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.walk

public class Linter private constructor(
  private val allowList: AllowList,
  private val path: Path,
) {

  // TODO(tsr): entirely skip parsing any script that has been configured to be ignored.
  private val _reports by lazy(LazyThreadSafetyMode.NONE) {
    val statements = getBuildScripts()
      .filterNot { buildScript -> allowList.isIgnoredBuildScript(relativePath(buildScript)) }
      .map { buildScript ->
        val extractor = BuildscriptTopLevelStatementExtractor.of(buildScript)
        val relativePath = relativePath(buildScript)

        Report(
          relativePath,
          allowList.forbiddenStatements(relativePath, extractor.getStatements()),
        )
      }
      .toList()

    ReportCollection(path, statements)
  }

  /**
   * We want the path to the build script relative to the root (`path`). In the case where `path` IS the build script,
   * then the relative path is just the last part of the `path`.
   */
  private fun relativePath(buildScript: Path): Path {
    return if (path == buildScript) path.last() else path.relativize(buildScript)
  }

  /** True if there are any violations in any of the parsed build scripts. */
  public fun hasErrors(): Boolean = _reports.reports.any { it.statements.isNotEmpty() }

  /** True if there are no violations in any of the parsed build scripts. */
  public fun hasNoErrors(): Boolean = !hasErrors()

  /** Returns a list of reports, one for each build script parsed. */
  public fun getReports(): ReportCollection = _reports

  private fun getBuildScripts(): Sequence<Path> {
    return if (path.isDirectory()) {
      require(path.exists()) { "$path does not exist!" }

      path.walk()
        .filter { it.isRegularFile() }
        .filter { it.exists() }
        .filter { it.name.endsWith("gradle.kts") }
    } else if (path.isRegularFile()) {
      require(path.exists()) { "$path does not exist!" }

      sequenceOf(path)
    } else {
      error("Expected either a regular file or a directory. Was '$path'.")
    }
  }

  public companion object {
    /**
     * Returns a [Linter][cash.recipes.lint.buildscripts.Linter], which reports problems found at [path] after filtering
     * out anything matching the [allowList]. [path] may be either a single build script (`.gradle.kts` file), or a
     * directory. If the latter, that directory will be walked recursively and every build script within will be linted.
     */
    public fun of(allowList: AllowList, path: Path): Linter {
      return Linter(allowList = allowList, path = path)
    }
  }
}
