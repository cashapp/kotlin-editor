package cash.recipes.lint.buildscripts.utils

import cash.recipes.lint.buildscripts.Logger

internal class TestLogger : Logger {

  private val messages = mutableListOf<String>()

  fun getMessages(): List<String> = messages

  override fun print(msg: String) {
    messages.add(msg)
    println(msg)
  }
}
