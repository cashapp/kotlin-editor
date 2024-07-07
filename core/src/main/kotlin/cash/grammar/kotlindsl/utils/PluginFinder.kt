package cash.grammar.kotlindsl.utils

import cash.grammar.kotlindsl.model.Plugin
import cash.grammar.kotlindsl.parse.Parser
import cash.grammar.kotlindsl.utils.Blocks.isPlugins
import cash.grammar.kotlindsl.utils.Context.leafRule
import com.squareup.cash.grammar.KotlinParser
import com.squareup.cash.grammar.KotlinParser.NamedBlockContext
import com.squareup.cash.grammar.KotlinParser.PostfixUnaryExpressionContext
import com.squareup.cash.grammar.KotlinParserBaseListener
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.TerminalNode
import java.io.InputStream
import java.nio.file.Path

/**
 * Scans Gradle Kotlin DSL scripts to find all plugin declarations.
 */
public class PluginFinder(
  private val input: CharStream,
  private val tokens: CommonTokenStream,
  private val parser: KotlinParser,
  private val errorListener: CollectingErrorListener
) : KotlinParserBaseListener() {

  /** Read-only view of the discovered [Plugin]s. */
  public val plugins: Set<Plugin>
    get() = discoveredPlugins.toSet()

  private val discoveredPlugins = mutableSetOf<Plugin>()

  private val blockStack = ArrayDeque<NamedBlockContext>()

  override fun enterNamedBlock(ctx: NamedBlockContext) {
    blockStack.addFirst(ctx)
  }

  /**
   * Finds plugins in plugins block
   */
  override fun exitNamedBlock(ctx: NamedBlockContext) {
    if (ctx.isPlugins) {
      ctx.statements().statement()
        .filterNot { it is TerminalNode }
        .map { it.leafRule() }
        .forEach { line ->
          PluginExtractor.extractFromBlock(line)?.let { plugin ->
            discoveredPlugins.add(plugin)
          }
        }
    }

    blockStack.removeFirst()
  }

  override fun exitPostfixUnaryExpression(ctx: PostfixUnaryExpressionContext) {
    if (!PluginExtractor.scriptLikeContext(blockStack)) return

    PluginExtractor.extractFromScript(ctx)?.let { plugin ->
      discoveredPlugins.add(plugin)
    }
  }

  public companion object {
    public fun of(buildScript: Path): PluginFinder {
      return of(Parser.readOnlyInputStream(buildScript))
    }

    public fun of(buildScript: String): PluginFinder {
      return of(buildScript.byteInputStream())
    }

    public fun of(buildScript: InputStream): PluginFinder {
      val errorListener = CollectingErrorListener()

      return Parser(
        file = buildScript,
        errorListener = errorListener,
        listenerFactory = { input, tokens, parser ->
          PluginFinder(
            input = input,
            tokens = tokens,
            parser = parser,
            errorListener = errorListener,
          )
        }
      ).listener()
    }
  }
}