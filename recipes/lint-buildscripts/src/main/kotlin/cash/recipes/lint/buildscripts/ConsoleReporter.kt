package cash.recipes.lint.buildscripts

import cash.recipes.lint.buildscripts.model.Position
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
    val forbiddenStatements = linter.getForbiddenStatements()

    val msg = if (forbiddenStatements.statements.isEmpty()) {
      "The build script '${forbiddenStatements.buildScript}' contains no forbidden statements."
    } else {
      buildString {
        val count = forbiddenStatements.statements.size
        val statement = if (count != 1) "statements" else "statement"

        appendLine("The build script '${forbiddenStatements.buildScript}' contains $count forbidden $statement:")
        appendLine()

        forbiddenStatements.statements.forEachIndexed { i, stmt ->
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
    }

    logger.print(msg)
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
