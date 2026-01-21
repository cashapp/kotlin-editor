package cash.recipes.lint.cli

import cash.recipes.lint.buildscripts.reporter.Logger
import com.github.ajalt.clikt.core.BaseCliktCommand

internal class ConsoleLogger(private val cliktCommand: BaseCliktCommand<*>) : Logger {
  override fun print(msg: String) {
    cliktCommand.echo(message = msg, trailingNewline = false)
  }

  override fun error(msg: String) {
    cliktCommand.echo(message = msg, trailingNewline = false, err = true)
  }
}
