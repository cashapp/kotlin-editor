package cash.grammar.kotlindsl.utils

import cash.grammar.kotlindsl.model.Plugin
import cash.grammar.kotlindsl.parse.Parser
import cash.grammar.kotlindsl.utils.Blocks.isPlugins
import cash.grammar.kotlindsl.utils.Context.leafRule
import cash.grammar.kotlindsl.utils.test.TestErrorListener
import com.squareup.cash.grammar.KotlinParser.NamedBlockContext
import com.squareup.cash.grammar.KotlinParser.PostfixUnaryExpressionContext
import com.squareup.cash.grammar.KotlinParserBaseListener
import org.antlr.v4.runtime.tree.TerminalNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class PluginExtractorTest {
  @Test fun `can extract all plugin types`() {
    val buildScript = """
      plugins {
        id("by-id")
        kotlin("jvm")
        application
        `kotlin-dsl`
        alias(libs.plugins.by.alias)
      }
      
      apply(plugin = "by-apply")
      apply(mapOf("plugin" to "by-map"))
    """.trimIndent()

    val scriptListener = Parser(
      file = buildScript,
      errorListener = TestErrorListener {
        throw RuntimeException("Syntax error: ${it?.message}", it)
      },
      listenerFactory = { _, _, _ -> TestListener() }
    ).listener()

    assertThat(scriptListener.plugins).containsExactly(
      Plugin(Plugin.Type.BLOCK_ID, "by-id"),
      Plugin(Plugin.Type.BLOCK_KOTLIN, "jvm"),
      Plugin(Plugin.Type.BLOCK_SIMPLE, "application"),
      Plugin(Plugin.Type.BLOCK_BACKTICK, "kotlin-dsl"),
      Plugin(Plugin.Type.BLOCK_ALIAS, "libs.plugins.by.alias"),
      Plugin(Plugin.Type.APPLY, "by-apply"),
      Plugin(Plugin.Type.APPLY, "by-map"),
    )
  }

  @Test fun `can extract all plugin types with configurations`() {
    val buildScript = """
      plugins {
        id("by-id") apply false
        kotlin("jvm") apply false
        application apply false
        `kotlin-dsl` apply false
        alias(libs.plugins.by.alias) apply false

        id("by-id-with-version") version libs.byId.get().version
        id("by-id-with-version-and-applied") version libs.byId.get().version apply false
      }
    """.trimIndent()

    val scriptListener = Parser(
      file = buildScript,
      errorListener = TestErrorListener {
        throw RuntimeException("Syntax error: ${it?.message}", it)
      },
      listenerFactory = { _, _, _ -> TestListener() }
    ).listener()

    assertThat(scriptListener.plugins).containsExactly(
      Plugin(Plugin.Type.BLOCK_ID, "by-id", applied = false),
      Plugin(Plugin.Type.BLOCK_KOTLIN, "jvm", applied = false),
      Plugin(Plugin.Type.BLOCK_SIMPLE, "application", applied = false),
      Plugin(Plugin.Type.BLOCK_BACKTICK, "kotlin-dsl", applied = false),
      Plugin(Plugin.Type.BLOCK_ALIAS, "libs.plugins.by.alias", applied = false),
      Plugin(Plugin.Type.BLOCK_ID, "by-id-with-version", version = "libs.byId.get().version"),
      Plugin(Plugin.Type.BLOCK_ID, "by-id-with-version-and-applied", version = "libs.byId.get().version", applied = false),
    )
  }

  private class TestListener : KotlinParserBaseListener() {

    private val blockStack = ArrayDeque<NamedBlockContext>()

    val plugins = mutableListOf<Plugin>()

    override fun enterNamedBlock(ctx: NamedBlockContext) {
      blockStack.addFirst(ctx)
    }

    override fun exitNamedBlock(ctx: NamedBlockContext) {
      if (ctx.isPlugins) {
        ctx.statements().statement()
          .filterNot { it is TerminalNode }
          .map { it.leafRule() }
          .forEach { line ->
            PluginExtractor.extractFromBlock(line)?.let { plugin ->
              plugins.add(plugin)
            }
          }
      }

      blockStack.removeFirst()
    }

    override fun exitPostfixUnaryExpression(ctx: PostfixUnaryExpressionContext) {
      if (!PluginExtractor.scriptLikeContext(blockStack)) return

      PluginExtractor.extractFromScript(ctx)?.let { plugin ->
        plugins.add(plugin)
      }
    }
  }
}
