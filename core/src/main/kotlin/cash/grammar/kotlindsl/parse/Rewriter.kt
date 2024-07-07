package cash.grammar.kotlindsl.parse

import cash.grammar.kotlindsl.utils.Whitespace
import com.squareup.cash.grammar.KotlinLexer
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.TokenStreamRewriter

/**
 * A subclass of ANTLR's TokenStreamRewriter that provides additional functionality.
 *
 * Note that as with the [Whitespace] helper class, this class makes a distinction between "white space"
 * and "blank space". Refer to the documentation on [Whitespace] for more details.
 */
public class Rewriter(
  private val commonTokens: CommonTokenStream
) : TokenStreamRewriter(commonTokens) {

  /**
   * Deletes all comments and whitespace (including newlines) "to the left of" [before].
   *
   * This is a complicated process because there can be a mix of whitespace, newlines (not
   * considered "whitespace" in this context), and comments, and we want to delete exactly as much
   * as necessary to "delete the line" -- nothing more, nothing less.
   */
  public fun deleteCommentsAndBlankSpaceToLeft(
    before: Token,
  ) {
    var comments = deleteCommentsToLeft(before)

    val ws = Whitespace.getBlankSpaceToLeft(commonTokens, before).onEach {
      delete(it)
    }

    if (comments == null && ws.isNotEmpty()) {
      comments = deleteCommentsToLeft(ws.last())
    }

    // TODO(tsr): it's unclear when to use `last()` vs `first()`. Sometimes the List<Token> seems
    //  like it is returned in reverse-order. We should resolve this once and for all.
    comments?.last()?.let { firstComment ->
      Whitespace.getWhitespaceToLeft(commonTokens, firstComment)
        ?.onEach { ws -> delete(ws) }
        ?.first()?.let { deleteNewlineToLeft(it) }
    }
  }

  /**
   * Deletes all comments "to the right of" [after], returning the list of comment tokens, if they
   * exist.
   */
  public fun deleteCommentsToLeft(
    before: Token,
  ): List<Token>? {
    // line or block comments
    return commonTokens
      .getHiddenTokensToLeft(before.tokenIndex, KotlinLexer.COMMENTS)
      ?.onEach { token ->
        delete(token)
      }
  }

  /**
   * Deletes all comments and whitespace (including newlines) "to the right of" [after]. Such
   * comments are assumed to start on the same line.
   *
   * This is a complicated process because there can be a mix of whitespace, newlines (not
   * considered "whitespace" in this context), and comments, and we want to delete exactly as much
   * as necessary to "delete the line" -- nothing more, nothing less.
   *
   * Note that this algorithm differs from [deleteCommentsAndBlankSpaceToLeft] because comments "to
   * the right of" a statement must start on the same line (no intervening newline characters).
   */
  public fun deleteCommentsAndBlankSpaceToRight(
    after: Token
  ) {
    deleteCommentsToRight(after)

    Whitespace.getWhitespaceToRight(commonTokens, after)?.forEach {
      delete(it)
    }
  }

  /**
   * Deletes all comments "to the right of" [after], returning the list of comment tokens, if they
   * exist.
   */
  public fun deleteCommentsToRight(
    after: Token,
  ): List<Token>? {
    // line or block comments
    return commonTokens
      .getHiddenTokensToRight(after.tokenIndex, KotlinLexer.COMMENTS)
      ?.onEach { token ->
        delete(token)
      }
  }

  /**
   * Delete _blank_ (spaces, tabs, and line breaks) "to the left of" [before], from the rewriter's
   * token stream.
   */
  public fun deleteBlankSpaceToLeft(before: Token) {
    Whitespace.getBlankSpaceToLeft(commonTokens, before).forEach { delete(it) }
  }

  /**
   * Delete _blank_ (spaces, tabs, and line breaks) "to the right of" [after], from the rewriter's
   * token stream.
   */
  public fun deleteBlankSpaceToRight(after: Token) {
    // TODO(tsr): this is problematic. Sometimes it deletes too much, other times not enough.
    //  we will suffer some extra whitespace to avoid syntax issues, which are worse. I will return
    //  to this when I have time.
    Whitespace.getBlankSpaceToRight(commonTokens, after)
      .drop(1)
      .forEach { delete(it) }
  }

  /**
   * Delete all whitespaces "to the left of" [before], from the rewriter's token stream.
   */
  public fun deleteWhitespaceToLeft(before: Token) {
    Whitespace.getWhitespaceToLeft(commonTokens, before)?.forEach { delete(it) }
  }

  /**
   * Delete newline "to the left of" [before], from the rewriter's token stream.
   */
  public fun deleteNewlineToLeft(before: Token) {
    if (before.tokenIndex - 1 > 0) {
      val previousToken = tokenStream.get(before.tokenIndex - 1)

      if (previousToken.type == KotlinLexer.NL) {
        delete(previousToken.tokenIndex)
      }
    }
  }

  /**
   * Delete newline "to the right of" [after], from the rewriter's token stream.
   */
  public fun deleteNewlineToRight(after: Token) {
    if (after.tokenIndex + 1 < tokenStream.size()) {
      val nextToken = tokenStream.get(after.tokenIndex + 1)

      if (nextToken.type == KotlinLexer.NL) {
        delete(nextToken.tokenIndex)
      }
    }
  }
}