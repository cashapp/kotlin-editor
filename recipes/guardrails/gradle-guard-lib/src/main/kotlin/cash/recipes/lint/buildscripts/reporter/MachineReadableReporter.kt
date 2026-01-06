package cash.recipes.lint.buildscripts.reporter

import cash.recipes.lint.buildscripts.Linter
import cash.recipes.lint.buildscripts.model.Report
import cash.recipes.lint.buildscripts.model.Statement

public class MachineReadableReporter private constructor(
  private val linter: Linter,
  private val logger: Logger,
) : Reporter {

  /**
   * Reports will look like this:
   * ```
   * grammar/build.gradle.kts:5 has forbidden statement plugins { ... }
   * core/build.gradle.kts:1 has forbidden statement plugins { ... }
   * core/build.gradle.kts:5 has forbidden statement tasks.named { ...
   * ```
   */
  public override fun buildReport(): String {
    val reports = linter.getReports()

    return if (reports.reports.all { it.statements.isEmpty() }) {
      // The case where there are no violations anywhere
      "None of the build scripts contain forbidden statements."
    } else if (reports.reports.size == 1) {
      // The case where there is only a single file analyzed
      val report = reports.reports.single()

      if (report.statements.isEmpty()) {
        "The build script '${report.buildScript}' contains no forbidden statements."
      } else {
        buildString { buildReport(report) }
      }
    } else {
      buildString {
        reports.reports
          .filter { report -> report.statements.isNotEmpty() }
          .forEach { report -> buildReport(report) }
      }
    }
  }

  public override fun printReport() {
    val msg = buildReport()
    logger.print(msg)
  }

  private fun StringBuilder.buildReport(report: Report) {
    report.statements.forEach { stmt ->
      appendLine("${report.buildScript}:${stmt.start.line} has forbidden statement ${stmt.richText()}")
    }
  }

  private fun Statement.richText(): String {
    return when (this) {
      is Statement.Assignment -> "$text …"
      is Statement.Declaration -> "$text …"
      is Statement.Expression -> "$text …"
      is Statement.Loop -> "$text …"
      is Statement.NamedBlock -> "$name { … }"
    }
  }

  public companion object {
    public fun of(linter: Linter, logger: Logger = SimpleLogger()): MachineReadableReporter {
      return MachineReadableReporter(linter, logger)
    }
  }
}
