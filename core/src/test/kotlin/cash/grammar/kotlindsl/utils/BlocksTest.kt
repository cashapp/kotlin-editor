package cash.grammar.kotlindsl.utils

import cash.grammar.kotlindsl.parse.Parser
import cash.grammar.kotlindsl.parse.process
import cash.grammar.kotlindsl.utils.Blocks.isRepositories
import cash.grammar.kotlindsl.utils.test.TestErrorListener
import com.squareup.cash.grammar.KotlinParser.NamedBlockContext
import com.squareup.cash.grammar.KotlinParser.SimpleIdentifierContext
import com.squareup.cash.grammar.KotlinParserBaseListener
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class BlocksTest {

  @Test fun `outermost block containing 'repositories' is 'subprojects'`() {
    val buildScript =
      """
        subprojects {
          // comments are ignored
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
      listenerFactory = { _, _, _ -> TestListener() }
    ).listener()

    assertThat(scriptListener.outermostBlock).isNotNull()
    assertThat(scriptListener.outermostBlock!!.name().text).isEqualTo(Blocks.SUBPROJECTS)
  }

  @Test fun `outermost block containing 'repositories' is 'buildscript'`() {
    val buildScript =
      """
        subprojects {
          apply(plugin = "kotlin")
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
      listenerFactory = { _, _, _ -> TestListener() }
    ).listener()

    assertThat(scriptListener.outermostBlock).isNotNull()
    assertThat(scriptListener.outermostBlock!!.name().text).isEqualTo(Blocks.BUILDSCRIPT)
  }

  @Test fun `can get enclosing blocks`() {
    val buildScript =
      """
        subprojects {
          apply(plugin = "kotlin")
          buildscript {
            repositories {}
          }
        }
        
        apply(mapOf("plugin" to "foo"))
      """.trimIndent()

    val scriptListener = Parser(
      file = buildScript,
      errorListener = TestErrorListener {
        throw RuntimeException("Syntax error: ${it?.message}", it)
      },
      listenerFactory = { _, _, _ -> TestListener() }
    ).listener()

    assertThat(scriptListener.applyByMapParentBlock).isNull()
    assertThat(scriptListener.repositoriesGrandParentBlock).isEqualTo(Blocks.SUBPROJECTS)
    assertThat(scriptListener.repositoriesParentBlock).isEqualTo(Blocks.BUILDSCRIPT)
  }

  @Test fun `iterating named blocks`() {
    val buildScript =
      """
        subprojects {
          buildscript {
          }
        }
        dependencies {
        }
        apply(mapOf("plugin" to "foo"))
        someOtherBlock {
        }
      """.trimIndent()

    val blocks = mutableListOf<String>()
    buildScript.process { script ->
      Blocks.forEachNamedBlock(script.statement()) { block ->
        blocks.add(block.name().text)
      }
    }

    assertThat(blocks).containsExactly(Blocks.SUBPROJECTS, Blocks.DEPENDENCIES, "someOtherBlock")
  }

  private class TestListener : KotlinParserBaseListener() {

    private val blockStack = ArrayDeque<NamedBlockContext>()
    var outermostBlock: NamedBlockContext? = null

    var repositoriesGrandParentBlock: String? = null
    var repositoriesParentBlock: String? = null
    var applyByMapParentBlock: String? = null

    override fun enterNamedBlock(ctx: NamedBlockContext) {
      blockStack.addFirst(ctx)
    }

    override fun exitNamedBlock(ctx: NamedBlockContext) {
      if (ctx.isRepositories) {
        outermostBlock = Blocks.getOutermostBlock(blockStack)
        repositoriesParentBlock = Blocks.enclosingNamedBlock(ctx) // buildscript
        repositoriesGrandParentBlock = Blocks.enclosingNamedBlock(
          ctx,
          Blocks.SUBPROJECTS,
        ) // subprojects
      }

      blockStack.removeFirst()
    }

    override fun enterSimpleIdentifier(ctx: SimpleIdentifierContext) {
      if (ctx.text == "mapOf") {
        applyByMapParentBlock = Blocks.enclosingNamedBlock(ctx) // null
      }
    }
  }
}
