package cash.grammar.kotlindsl.utils

import com.squareup.cash.grammar.KotlinLexer
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.misc.Interval

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
   * Returns the indentation in this input, based on the assumption that the first indentation
   * discovered is the common indent level. If no indent discovered (which could happen if this
   * input contains only top-level statements), defaults to [min].
   */
  public fun computeIndent(
    tokens: CommonTokenStream,
    input: CharStream,
    min: String = "  ",
  ): String {
    // We need at least two tokens for this to make sense.
    if (tokens.size() < 2) return min

    val start = tokens.get(0).startIndex
    val stop = tokens.get(tokens.size() - 1).stopIndex

    if (start == -1 || stop == -1) return min

    // Kind of a gross implementation, but a starting point that works -- can optimize later.
    input.getText(Interval.of(start, stop)).lineSequence().forEach { line ->
      var indent = ""
      // a line might contain JUST whitespace -- we don't want to count these.
      var nonEmptyLine = false

      for (c in line.toCharArray()) {
        // Avoid pulling indentation from comments
        if (line.startsWith(" *")) continue

        if (c == ' ' || c == '\t') {
          indent += c
        } else {
          nonEmptyLine = true
          break
        }
      }

      if (nonEmptyLine && indent.isNotEmpty()) return indent
    }

    return min
  }

  /**
   * Use this in conjunction with [trimGently] to maintain original end-of-file formatting.
   */
  public fun countTerminalNewlines(tokens: CommonTokenStream): Int {
    var count = 0

    // We use size - 2 because we skip the EOF token, which always exists.
    for (i in tokens.size() - 2 downTo 0) {
      val t = tokens.get(i)
      if (t.type == KotlinLexer.NL) count++
      else break
    }
    return count
  }

  /**
   * Use this in conjunction with [countTerminalNewlines] to maintain original end-of-file
   * formatting.
   */
  public fun String.trimGently(terminalNewlines: Int = 0): String {
    return trim() + "\n".repeat(terminalNewlines)
  }
}
