package cash.recipes.lint.buildscripts

import cash.recipes.lint.buildscripts.model.Statements
import cash.recipes.lint.buildscripts.parser.BuildscriptTopLevelStatementExtractor
import java.nio.file.Path

public class Linter private constructor(
  private val extractor: Lazy<BuildscriptTopLevelStatementExtractor>,
  private val buildScript: Path,
  private val allowList: AllowList,
) {

  public fun getForbiddenStatements(): Statements {
    return Statements(
      buildScript,
      allowList.forbiddenStatements(extractor.value.getStatements()),
    )
  }

  public companion object {
    public fun of(buildScript: Path, allowList: AllowList): Linter {
      val extractor = lazy(LazyThreadSafetyMode.NONE) { (BuildscriptTopLevelStatementExtractor.of(buildScript)) }

      return Linter(extractor, buildScript, allowList)
    }
  }
}
