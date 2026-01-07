package cash.recipes.lint.buildscripts

import cash.recipes.lint.buildscripts.model.Statements
import cash.recipes.lint.buildscripts.parser.BuildscriptTopLevelStatementExtractor
import java.nio.file.Path

public class Linter private constructor(
  private val extractor: Lazy<BuildscriptTopLevelStatementExtractor>,
  private val allowList: AllowList,
  private val buildScript: Path,
  private val root: Path?,
) {

  public fun getForbiddenStatements(): Statements {
    val printablePath = root?.relativize(buildScript) ?: buildScript

    return Statements(
      printablePath,
      allowList.forbiddenStatements(extractor.value.getStatements()),
    )
  }

  public companion object {
    public fun of(allowList: AllowList, buildScript: Path, root: Path? = null): Linter {
      val extractor = lazy(LazyThreadSafetyMode.NONE) { (BuildscriptTopLevelStatementExtractor.of(buildScript)) }

      return Linter(extractor, allowList = allowList, buildScript = buildScript, root = root)
    }
  }
}
