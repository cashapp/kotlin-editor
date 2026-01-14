package cash.recipes.lint.buildscripts.reporter

public interface Reporter {
  public fun buildReport(): String
  public fun printReport()
}
