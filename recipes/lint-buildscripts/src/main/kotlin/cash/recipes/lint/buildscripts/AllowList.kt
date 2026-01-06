package cash.recipes.lint.buildscripts

import cash.recipes.lint.buildscripts.model.Statement

public class AllowList private constructor(private val names: Set<String>) {

  public fun forbiddenStatements(statements: List<Statement>): List<Statement> {
    return statements.filterNot { it.text in names }
  }

  public companion object {
    public fun of(vararg names: String): AllowList {
      return AllowList(names.toSet())
    }
  }
}
