package cash.recipes.lint.buildscripts.reporter

import cash.recipes.lint.buildscripts.Linter
import cash.recipes.lint.buildscripts.model.Position
import cash.recipes.lint.buildscripts.model.Report
import cash.recipes.lint.buildscripts.model.Statement

public class HumanReadableReporter private constructor(
  private val linter: Linter,
  private val logger: Logger,
) : Reporter {

  /**
   * Reports will look like this:
   * ```
   * Analysis complete. In path /Users/trobalik/development/cashapp/kotlin-editor, found:
   * - 0 without any violations.
   * - 12 with violations.
   *
   * The build script 'grammar/build.gradle.kts' contains 1 forbidden statement:
   *
   * 1: plugins { … (named block)
   *    Start: (Line: 1, Column: 0)
   *    End:   (Line: 3, Column: 0)
   *
   * The build script 'core/build.gradle.kts' contains 2 forbidden statements:
   *
   * 1: plugins { … (named block)
   *    Start: (Line: 1, Column: 0)
   *    End:   (Line: 3, Column: 0)
   * 2: dependencies { … (named block)
   *    Start: (Line: 5, Column: 0)
   *    End:   (Line: 12, Column: 0)
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
        val filesWithViolations = reports.reports.count { it.statements.isNotEmpty() }
        val filesWithoutViolations = reports.reports.size - filesWithViolations

        appendLine("Analysis complete. Found:")
        appendLine("- $filesWithoutViolations without any violations.")
        appendLine("- $filesWithViolations with violations.")
        appendLine()

        val nonEmptyReports = reports.reports.filter { it.statements.isNotEmpty() }
        val nonEmptyReportsCount = nonEmptyReports.size

        nonEmptyReports.forEachIndexed { i, report ->
          buildReport(report)

          // Add an empty line separating items, but don't at the end.
          if (i < nonEmptyReportsCount - 1) {
            appendLine()
          }
        }
      }
    }
  }

  public override fun printReport() {
    val msg = buildReport()
    logger.print(msg)
  }

  private fun StringBuilder.buildReport(report: Report) {
    val count = report.statements.size
    val statement = if (count != 1) "statements" else "statement"

    appendLine("The build script '${report.buildScript}' contains $count forbidden $statement:")
    appendLine()

    report.statements.forEachIndexed { i, stmt ->
      val number = "${i + 1}: "
      appendLine("$number${stmt.richText()}")

      append(" ".repeat(number.length))
      appendLine(stmt.startIndex())
      append(" ".repeat(number.length))
      appendLine(stmt.stopIndex())

      // Add an empty line separating items, but don't at the end.
      if (i < count - 1) {
        appendLine()
      }
    }
  }

  private fun Statement.richText(): String {
    return when (this) {
      is Statement.Assignment -> "$text … (assignment)"
      is Statement.Declaration -> "$text … (declaration)"
      is Statement.Expression -> "$text … (expression)"
      is Statement.Loop -> "$text … (loop)"
      is Statement.NamedBlock -> "$name { … (named block)"
    }
  }

  private fun Statement.startIndex(): String = "Start: ${start.asIndex()}"
  private fun Statement.stopIndex(): String = "End:   ${stop.asIndex()}"
  private fun Position.asIndex(): String = "(Line: $line, Column: $positionInLine)"

  public companion object {
    public fun of(linter: Linter, logger: Logger = SimpleLogger()): HumanReadableReporter {
      return HumanReadableReporter(linter, logger)
    }
  }
}
