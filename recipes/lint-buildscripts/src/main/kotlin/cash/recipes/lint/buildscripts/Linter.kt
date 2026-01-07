package cash.recipes.lint.buildscripts

import cash.recipes.lint.buildscripts.model.Report
import cash.recipes.lint.buildscripts.model.ReportCollection
import cash.recipes.lint.buildscripts.parser.BuildscriptTopLevelStatementExtractor
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.walk

public class Linter private constructor(
  private val allowList: AllowList,
  private val buildScript: Path?,
  private val root: Path?,
) {

  private val _reports by lazy(LazyThreadSafetyMode.NONE) {
    val statements = getBuildScripts()
      .map { buildScript ->
        val printablePath = root?.relativize(buildScript) ?: buildScript

        val extractor = BuildscriptTopLevelStatementExtractor.of(buildScript)

        Report(
          printablePath,
          allowList.forbiddenStatements(extractor.getStatements()),
        )
      }
      .toList()

    ReportCollection(root, statements)
  }

  /** True if there are no violations in any of the parsed build scripts. */
  public fun hasErrors(): Boolean = _reports.reports.all { it.statements.isEmpty() }

  /** Returns a list of reports, one for each build script parsed. */
  public fun getReports(): ReportCollection = _reports

  // TODO: simplify this once I'm certain of the supported use-cases
  private fun getBuildScripts(): Sequence<Path> {
    return if (root?.isDirectory() == true && buildScript == null) {
      require(root.exists()) { "$root does not exist!" }

      root.walk()
        .filter { it.name.endsWith("gradle.kts") }
        .filter { it.exists() }
    } else if (buildScript != null) {
      require(buildScript.exists()) { "$buildScript does not exist!" }
      sequenceOf(buildScript)
    } else {
      error("`buildScript` must be non-null, or `root` must be a directory.")
    }
  }

  public companion object {
    public fun of(allowList: AllowList, buildScript: Path, root: Path? = null): Linter {
      return Linter(allowList = allowList, buildScript = buildScript, root = root)
    }

    public fun of(allowList: AllowList, root: Path): Linter {
      return Linter(allowList = allowList, root = root, buildScript = null)
    }
  }
}
