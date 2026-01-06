package cash.recipes.lint.cli

import cash.recipes.lint.buildscripts.reporter.Logger

internal class Logger : Logger {
  override fun print(msg: String) {
    println(msg)
  }

  override fun error(msg: String) {
    System.err.println(msg)
  }
}