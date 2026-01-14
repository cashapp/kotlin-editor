package cash.recipes.lint.buildscripts.model

/**
 * A collection of [reports], each for a single [Report.buildScript]. [root] is at the root of the directory hierarchy
 * that contains each of these build scripts. May be null.
 */
public data class ReportCollection(public val reports: List<Report>) {
  public fun hasErrors(): Boolean = reports.any { it.statements.isNotEmpty() }
}
