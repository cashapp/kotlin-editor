package cash.grammar.kotlindsl.utils

import cash.grammar.kotlindsl.parse.Parser
import cash.grammar.kotlindsl.utils.Blocks.isBuildscript
import cash.grammar.kotlindsl.utils.Blocks.isSubprojects
import cash.grammar.kotlindsl.utils.test.TestErrorListener
import com.squareup.cash.grammar.KotlinParser
import com.squareup.cash.grammar.KotlinParser.NamedBlockContext
import com.squareup.cash.grammar.KotlinParserBaseListener
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.Token
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.Test

internal class WhitespaceTest {

  @Test fun `can extract newlines`() {
    val buildScript =
      """
        plugins {
          id("kotlin")
        }
        
        subprojects {
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
      listenerFactory = { _, tokens, _ -> TestListener(tokens) }
    ).listener()

    // There are two newlines preceding the 'subprojects' block
    assertThat(scriptListener.newlines).isNotNull()
    assertThat(scriptListener.newlines!!.size).isEqualTo(2)
    assertThat(scriptListener.newlines!!)
      .extracting({ it.text })
      .allSatisfy { assertThat(it).isEqualTo(tuple("\n")) }
  }

  @Test fun `can extract whitespace`() {
    val buildScript =
      """
        plugins {
          id("kotlin")
        }
        
        subprojects {
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
      listenerFactory = { _, tokens, _ -> TestListener(tokens) }
    ).listener()

    // There are two spaces preceding the 'buildscript' block (on the same line)
    assertThat(scriptListener.whitespace).isNotNull()
    assertThat(scriptListener.whitespace!!.size).isEqualTo(2)
    assertThat(scriptListener.whitespace!!)
      .extracting({ it.text })
      .allSatisfy { assertThat(it).isEqualTo(tuple(" ")) }
  }

  @Test fun `gets trailing newlines for buildscript`() {
    val buildScript =
      """
        plugins {
          id("kotlin")
        }
        
        subprojects {
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
      listenerFactory = { _, tokens, _ -> TestListener(tokens) }
    ).listener()

    assertThat(scriptListener.trailingBuildscriptNewlines).isEqualTo(1)
  }

  @Test fun `gets trailing newlines for kotlin file`() {
    val file =
      """
        class Foo {
        }
        
      """.trimIndent()

    val scriptListener = Parser(
      file = file.byteInputStream(),
      errorListener = TestErrorListener {
        throw RuntimeException("Syntax error: ${it?.message}", it)
      },
      startRule = { parser -> parser.kotlinFile() },
      listenerFactory = { _, tokens, _ -> TestListener(tokens) }
    ).listener()

    assertThat(scriptListener.trailingKotlinFileNewlines).isEqualTo(1)
  }

  private class TestListener(
    private val tokens: CommonTokenStream
  ) : KotlinParserBaseListener() {

    var newlines: List<Token>? = null
    var whitespace: List<Token>? = null
    var trailingBuildscriptNewlines = 0
    var trailingKotlinFileNewlines = 0

    override fun enterScript(ctx: KotlinParser.ScriptContext) {
      trailingBuildscriptNewlines = Whitespace.countTerminalNewlines(ctx, tokens)
    }

    override fun enterKotlinFile(ctx: KotlinParser.KotlinFileContext) {
      trailingKotlinFileNewlines = Whitespace.countTerminalNewlines(ctx, tokens)
    }

    override fun exitNamedBlock(ctx: NamedBlockContext) {
      if (ctx.isSubprojects) {
        newlines = Whitespace.getBlankSpaceToLeft(tokens, ctx)
      }
      if (ctx.isBuildscript) {
        whitespace = Whitespace.getWhitespaceToLeft(tokens, ctx)
      }
    }
  }
}
