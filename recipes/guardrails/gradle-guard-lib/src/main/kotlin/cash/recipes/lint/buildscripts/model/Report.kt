package cash.recipes.lint.buildscripts.model

import java.nio.file.Path

/** A report of [statements] for a single [buildScript]. */
public data class Report(
  public val buildScript: Path,
  public val statements: List<Statement>,
)
