package cash.grammar.kotlindsl.utils

import com.squareup.cash.grammar.KotlinParser.LineStringLiteralContext
import com.squareup.cash.grammar.KotlinParser.LiteralConstantContext
import com.squareup.cash.grammar.KotlinParser.PostfixUnaryExpressionContext
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.misc.Interval

public object Context {

  /**
   * Returns the "full text", from [input], represented by [this][ParserRuleContext]. The full text includes tokens that
   * are sent to hidden channels by the lexer. cf. [ParserRuleContext.text][ParserRuleContext.getText], which only
   * considers tokens which have been added to the parse tree (i.e., not comments or whitespace).
   *
   * Returns null if `this` has a null [ParserRuleContext.start] or [ParserRuleContext.stop], which can happen when,
   * e.g., `this` is a [ScriptContext][com.squareup.cash.grammar.KotlinParser.ScriptContext]. (I don't fully understand
   * why those tokens might be null.)
   */
  public fun ParserRuleContext.fullText(input: CharStream): String? {
    val a = start?.startIndex ?: return null
    val b = stop?.stopIndex ?: return null

    return input.getText(Interval.of(a, b))
  }

  /**
   * Given a [ParserRuleContext] that has a single child that has a single child... return the leaf
   * node terminal [ParserRuleContext].
   */
  public fun ParserRuleContext.leafRule(): ParserRuleContext {
    var tree = this
    while (tree.childCount == 1) {
      val child = tree.getChild(0)

      // Might also be a TerminalNode, which is essentially a lexer rule token
      // (we want a parser rule token).
      if (child !is ParserRuleContext) {
        break
      }

      tree = child
    }

    return tree
  }

  /**
   * Returns the literal text this [ctx] refers to, iff it is a [LineStringLiteralContext]
   * (potentially via many intermediate parser rules).
   */
  public fun literalText(ctx: ParserRuleContext): String? {
    return (ctx.leafRule() as? LineStringLiteralContext)
      ?.lineStringContent()
      ?.get(0)
      ?.LineStrText()
      ?.text
  }

  /**
   * Returns the literal boolean this [ctx] refers to, iff it is a [LiteralConstantContext]
   * (potentially via many intermediate parser rules).
   */
  public fun literalBoolean(ctx: ParserRuleContext): Boolean? {
    return (ctx.leafRule() as? LiteralConstantContext)
      ?.BooleanLiteral()
      ?.text
      ?.toBoolean()
  }


  public fun aliasText(ctx: ParserRuleContext): String? {
    return (ctx.leafRule() as? PostfixUnaryExpressionContext)?.text
  }
}
