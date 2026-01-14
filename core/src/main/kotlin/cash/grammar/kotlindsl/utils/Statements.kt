package cash.grammar.kotlindsl.utils

/**
 * A helper class for managing depth when navigating nested
 * [statements][com.squareup.cash.grammar.KotlinParser.StatementContext].
 */
public class Statements {

  private var level = 0

  /**
   * This method should be called at the start of the
   * [KotlinParserListener.enterStatement][com.squareup.cash.grammar.KotlinParserListener.enterStatement] method.
   */
  public fun onEnterStatement() {
    level++
  }

  /**
   * This method should be called at the end of the
   * [KotlinParserListener.exitStatement][com.squareup.cash.grammar.KotlinParserListener.exitStatement] method.
   */
  public fun onExitStatement() {
    level--
  }

  /**
   * Returns true if we're inside a top-level statement and _not_ in any nested statement therein.
   *
   * Implementation note: this method will only function as expected if the contracts described at [onEnterStatement]
   * and [onExitStatement] are followed.
   */
  public fun isTopLevel(): Boolean = level == 1
}
