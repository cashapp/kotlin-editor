package cash.grammar.kotlindsl.utils

import com.squareup.cash.grammar.KotlinLexer
import com.squareup.cash.grammar.KotlinParser.KotlinFileContext
import com.squareup.cash.grammar.KotlinParser.ScriptContext
import com.squareup.cash.grammar.KotlinParser.SemiContext
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token

/**
 * Utilities for working with whitespace, including newlines, carriage returns, etc.
 *
 * Note that this class distinguishes between "blank space" and "white space". The former includes
 * newlines while the latter does not. This is important because newlines are significant syntactic
 * elements in Kotlin; they are treated as statement terminators similar to semicolons in Java.
 * In some cases removal of newlines can change the semantics of the code so care must be taken to
 * use the appropriate method.
 */
public object Whitespace {

  /**
   * Returns a list of [Token]s, "to the left of" [before], from [tokens], that are _blank_
   * according to [String.isBlank].
   */
  public fun getBlankSpaceToLeft(
    tokens: CommonTokenStream,
    before: ParserRuleContext
  ): List<Token> = getBlankSpaceToLeft(tokens, before.start)

  /**
   * Returns a list of [Token]s, "to the left of" [before], from [tokens], that are _blank_
   * according to [String.isBlank].
   */
  public fun getBlankSpaceToLeft(tokens: CommonTokenStream, before: Token): List<Token> {
    return buildList {
      var index = before.tokenIndex - 1

      if (index <= 0) return@buildList

      var next = tokens.get(index)

      while (next.text.isBlank()) {
        add(next)
        next = tokens.get(--index)
      }
    }
  }

  /**
   * Returns a list of [Token]s, "to the right of" [after], from [tokens], that are _blank_
   * according to [String.isBlank].
   */
  public fun getBlankSpaceToRight(tokens: CommonTokenStream, after: Token): List<Token> {
    return buildList {
      var index = after.tokenIndex + 1

      if (index >= tokens.size()) return@buildList

      var next = tokens.get(index)

      while (next.text.isBlank()) {
        add(next)
        next = tokens.get(++index)
      }
    }
  }

  /**
   * Returns a list of [Token]s, "to the left of" [before], from [tokens], that are "whitespace".
   * Whitespace characters match the following lexer rule, from `KotlinLexer`:
   *
   * ```
   * [\u0020\u0009\u000C]
   * ```
   *
   * Returns `null` if there is no whitespace to the left of [before].
   *
   * nb to maintainers: do _not_ change this to an empty list instead of null. There's a difference!
   */
  public fun getWhitespaceToLeft(
    tokens: CommonTokenStream,
    before: ParserRuleContext
  ): List<Token>? = getWhitespaceToLeft(tokens, before.start)

  /**
   * Returns a list of [Token]s, "to the left of" [before], from [tokens], that are "whitespace".
   * Whitespace characters match the following lexer rule, from `KotlinLexer`:
   *
   * ```
   * [\u0020\u0009\u000C]
   * ```
   *
   * Returns `null` if there is no whitespace to the left of [before].
   *
   * nb to maintainers: do _not_ change this to an empty list instead of null. There's a difference!
   */
  public fun getWhitespaceToLeft(tokens: CommonTokenStream, before: Token): List<Token>? {
    return tokens.getHiddenTokensToLeft(before.tokenIndex, KotlinLexer.WHITESPACE)
  }

  /**
   * Returns a list of [Token]s, "to the left of" [before], from [tokens], that are "whitespace".
   * Whitespace characters match the following lexer rule, from `KotlinLexer`:
   *
   * ```
   * [\u0020\u0009\u000C]
   * ```
   *
   * Returns `null` if there is no whitespace to the left of [before].
   *
   * nb to maintainers: do _not_ change this to an empty list instead of null. There's a difference!
   */
  public fun getWhitespaceToRight(tokens: CommonTokenStream, before: Token): List<Token>? {
    return tokens.getHiddenTokensToRight(before.tokenIndex, KotlinLexer.WHITESPACE)
  }

  /**
   * Use this in conjunction with [trimGently] to maintain original end-of-file formatting.
   */
  public fun countTerminalNewlines(ctx: ScriptContext, tokens: CommonTokenStream): Int {
    return countTerminalNewlines(ctx as ParserRuleContext, tokens)
  }

  /**
   * Use this in conjunction with [trimGently] to maintain original end-of-file formatting.
   */
  public fun countTerminalNewlines(ctx: KotlinFileContext, tokens: CommonTokenStream): Int {
    return countTerminalNewlines(ctx as ParserRuleContext, tokens)
  }

  private fun countTerminalNewlines(ctx: ParserRuleContext, tokens: CommonTokenStream): Int {
    return ctx.children
      // Start iterating from EOF
      .reversed()
      .asSequence()
      // Drop `EOF` (every file must have this)
      .drop(1)
      // Take only the "semis", which is a semi-colon or a newline, followed by 0 or more newlines
      .takeWhile { parseTree ->
        // Because comments are not part of the parse tree (they are shunted to a "hidden" channel),
        // we need to check for them. Otherwise, we'll "detect" too many newlines at the end of a
        // file, when that file has only comments and newlines at the end.
        val hasNoComments = if (parseTree is ParserRuleContext) {
          val toLeft = parseTree.stop?.let { stop ->
            tokens.getHiddenTokensToLeft(stop.tokenIndex).orEmpty().isEmpty()
          } ?: true
          val toRight = parseTree.stop?.let { stop ->
            tokens.getHiddenTokensToRight(stop.tokenIndex).orEmpty().isEmpty()
          } ?: true

          toLeft && toRight
        } else {
          true
        }

        parseTree.javaClass == SemiContext::class.java && hasNoComments
      }
      .filterIsInstance<SemiContext>()
      // This is the "a newline, followed by 0 or more newlines"
      .flatMap { it.NL() }
      .count()
  }

  /**
   * Use this in conjunction with [countTerminalNewlines] to maintain original end-of-file
   * formatting.
   */
  public fun String.trimGently(terminalNewlines: Int = 0): String {
    return trim() + "\n".repeat(terminalNewlines)
  }
}
