package cash.recipes.lint.buildscripts.reporter

public class SimpleLogger : Logger {
  override fun print(msg: String) {
    println(msg)
  }
}
