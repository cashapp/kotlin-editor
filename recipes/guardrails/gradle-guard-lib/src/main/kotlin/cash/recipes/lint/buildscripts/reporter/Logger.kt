package cash.recipes.lint.buildscripts.reporter

public interface Logger {
  public fun print(msg: String)

  public fun error(msg: String) {
    print(msg)
  }
}
