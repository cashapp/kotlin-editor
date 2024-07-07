package cash.grammar.kotlindsl.utils

import cash.grammar.kotlindsl.parse.Parser
import cash.grammar.kotlindsl.utils.Blocks.isSubprojects
import cash.grammar.kotlindsl.utils.Context.fullText
import cash.grammar.kotlindsl.utils.test.TestErrorListener
import com.squareup.cash.grammar.KotlinParser.NamedBlockContext
import com.squareup.cash.grammar.KotlinParserBaseListener
import org.antlr.v4.runtime.CharStream
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class ContextTest {

  /**
   * This test demonstrates the difference between
   * [ParserRuleContext.text][org.antlr.v4.runtime.ParserRuleContext.getText] and
   * [ctx.fullText(input)][Context.fullText].
   */
  @Test fun `fullText contains actual complete user text`() {
    val buildScript =
      """
        subprojects {
          // comments and whitespace are ignored by `ParseRuleContext.text`
          buildscript {
            repositories {}
          }
        }
      """.trimIndent()

    val scriptListener = Parser(
      file = buildScript,
      errorListener = TestErrorListener {
        throw RuntimeException("Syntax error: ${it?.message}", it)
      },
      listenerFactory = { input, _, _ -> TestListener(input) }
    ).listener()

    assertThat(scriptListener.text).isEqualTo(
      """
        subprojects{
  
        buildscript{
        repositories{}
        }
        }
      """.trimIndent()
    )
    assertThat(scriptListener.fullText).isEqualTo(
      """
        subprojects {
          // comments and whitespace are ignored by `ParseRuleContext.text`
          buildscript {
            repositories {}
          }
        }
      """.trimIndent()
    )
  }

  private class TestListener(
    private val input: CharStream
  ) : KotlinParserBaseListener() {

    var text = ""
    var fullText = ""

    override fun exitNamedBlock(ctx: NamedBlockContext) {
      if (ctx.isSubprojects) {
        text = ctx.text
        fullText = ctx.fullText(input)!!
      }
    }
  }
}
