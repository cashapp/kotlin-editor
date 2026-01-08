package cash.recipes.lint.buildscripts

import cash.recipes.lint.buildscripts.model.Position
import cash.recipes.lint.buildscripts.model.Report
import cash.recipes.lint.buildscripts.model.Statement

public interface Logger {
  public fun print(msg: String)
}

public class SimpleLogger : Logger {
  override fun print(msg: String) {
    println(msg)
  }
}

public class ConsoleReporter private constructor(private val linter: Linter, private val logger: Logger) {

  public fun printReport() {
    val reports = linter.getReports()

    // TODO: clean this up

    // The case where there are no violations anywhere
    if (reports.reports.all { it.statements.isEmpty() }) {
      logger.print("None of the build scripts found in ${reports.root} contain forbidden statements.")
      return
    }

    // The case where there is only a single file analyzed
    if (reports.reports.size == 1) {
      val report = reports.reports.single()

      val msg = if (report.statements.isEmpty()) {
        "The build script '${report.buildScript}' contains no forbidden statements."
      } else {
        buildString { buildReport(report) }
      }

      logger.print(msg)
      return
    }

    // The most general case where there are multiple files analyzed
    val msg = buildString {
      val filesWithViolations = reports.reports.count { it.statements.isNotEmpty() }
      val filesWithoutViolations = reports.reports.size - filesWithViolations

      appendLine("Analysis complete. In path ${reports.root!!}, found:")
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
      is Statement.NamedBlock -> "$text { … (named block)"
    }
  }

  private fun Statement.startIndex(): String = "Start: ${start.asIndex()}"
  private fun Statement.stopIndex(): String = "End:   ${stop.asIndex()}"
  private fun Position.asIndex(): String = "(Line: $line, Column: $positionInLine)"

  public companion object {
    public fun of(linter: Linter, logger: Logger = SimpleLogger()): ConsoleReporter {
      return ConsoleReporter(linter, logger)
    }
  }
}
