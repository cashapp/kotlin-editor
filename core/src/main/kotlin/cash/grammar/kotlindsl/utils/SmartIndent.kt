package cash.grammar.kotlindsl.utils

import com.squareup.cash.grammar.KotlinParserBaseListener
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext

/**
 * Helper class for managing smart indents. Defaults to two spaces. Attempts to match whatever
 * indent is used by the source file from which [tokens] was taken.
 */
public class SmartIndent(private val tokens: CommonTokenStream) {

  // We use a default of two spaces, but update it at most once later on.
  private var smartIndentSet = false
  private var indent = "  "

  /** Call this sometime after [setIndent] has been called. */
  public fun getSmartIndent(): String = indent

  /** Call this from [KotlinParserBaseListener.enterStatement]. */
  public fun setIndent(ctx: ParserRuleContext) {
    if (smartIndentSet) return

    Whitespace.getWhitespaceToLeft(tokens, ctx.start)
      ?.joinToString(separator = "") { token -> token.text }
      ?.let { ws ->
        smartIndentSet = true
        indent = ws
      }
  }
}
