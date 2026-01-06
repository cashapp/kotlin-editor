package cash.grammar.kotlindsl.utils

/**
 * TODO: docs.
 */
public class Statements {

  private var level = 0

  public fun onEnterStatement() {
    level++
  }

  public fun onExitStatement() {
    level--
  }

  public fun isTopLevel(): Boolean = level == 1
}
