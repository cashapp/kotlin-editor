package cash.recipes.plugins

import cash.grammar.kotlindsl.model.Plugin
import cash.grammar.kotlindsl.parse.BuildScriptParseException
import cash.grammar.kotlindsl.parse.Parser
import cash.grammar.kotlindsl.parse.Rewriter
import cash.grammar.kotlindsl.utils.Blocks.isPlugins
import cash.grammar.kotlindsl.utils.CollectingErrorListener
import cash.grammar.kotlindsl.utils.Context.leafRule
import cash.grammar.kotlindsl.utils.PluginExtractor
import cash.grammar.kotlindsl.utils.SmartIndent
import cash.grammar.kotlindsl.utils.Whitespace
import cash.grammar.kotlindsl.utils.Whitespace.trimGently
import cash.grammar.utils.ifNotEmpty
import cash.recipes.plugins.exception.NonNormalizedScriptException
import com.squareup.cash.grammar.KotlinParser.NamedBlockContext
import com.squareup.cash.grammar.KotlinParser.PostfixUnaryExpressionContext
import com.squareup.cash.grammar.KotlinParser.ScriptContext
import com.squareup.cash.grammar.KotlinParser.StatementContext
import com.squareup.cash.grammar.KotlinParserBaseListener
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.TerminalNode
import java.io.InputStream
import java.nio.file.Path

/**
 * Manages the addition and removal of plugins in a Gradle build script that has been normalized by [PluginNormalizer].
 * This class ensures that plugins are applied using the standardized formats: [Plugin.Type.BLOCK_ID] or [Plugin.Type.BLOCK_SIMPLE].
 *
 * If the script is not normalized, a [NonNormalizedScriptException] is thrown.
 *
 * @param tokens the token stream that represents the entire input script, used for rewriting the script.
 * @param errorListener an error listener that collects parsing errors during the operation.
 * @param pluginsToAdd A set of plugin IDs to be added to the script. Existing plugins are ignored. Plugins are added in the [Plugin.Type.BLOCK_ID] format.
 * @param pluginsToRemove A set of plugin IDs to be removed from the script. This removal includes plugins in both [Plugin.Type.BLOCK_ID] and [Plugin.Type.BLOCK_SIMPLE] formats.
 *
 * @throws NonNormalizedScriptException if the script has not been normalized.
 * @throws IllegalArgumentException if conflicting plugins are found in both the pluginsToAdd and pluginsToRemove sets.
 * @throws BuildScriptParseException if the script cannot be parsed.
 */
public class PluginMutator private constructor(
  private val tokens: CommonTokenStream,
  private val errorListener: CollectingErrorListener,
  private val pluginsToAdd: Set<String>,
  private val pluginsToRemove: Set<String>,
) : KotlinParserBaseListener() {

  private val rewriter = Rewriter(tokens)
  private val smartIndent = SmartIndent(tokens)
  private var terminalNewlines = 0

  private val blockStack = ArrayDeque<NamedBlockContext>()
  private var pluginsBlock: NamedBlockContext? = null

  private val pluginIds = mutableSetOf<String>()

  public fun getPluginIds(): Set<String> = pluginIds

  @Throws(BuildScriptParseException::class, NonNormalizedScriptException::class)
  public fun rewritten(): String {
    errorListener.getErrorMessages().ifNotEmpty {
      throw BuildScriptParseException.withErrors(it)
    }

    return rewriter.text.trimGently(terminalNewlines)
  }

  override fun enterScript(ctx: ScriptContext) {
    terminalNewlines = Whitespace.countTerminalNewlines(ctx, tokens)
  }

  override fun enterNamedBlock(ctx: NamedBlockContext) {
    blockStack.addFirst(ctx)
  }

  @Throws(NonNormalizedScriptException::class)
  override fun exitNamedBlock(ctx: NamedBlockContext) {
    if (ctx.isPlugins) {
      pluginsBlock = ctx

      val blockPluginsContext = ctx.statements().statement()
        .filterNot { it is TerminalNode }
        .map { it.leafRule() }

      val appliedPluginsWithContext = blockPluginsContext.mapNotNull { context ->
        PluginExtractor.extractFromBlock(context)?.let { plugin ->
          pluginIds += plugin.id
          plugin to context
        }
      }.toMap()

      val appliedPlugins = appliedPluginsWithContext.keys.toList()

      // Error if the script is not normalized
      appliedPlugins.forEach { errorIfScriptNotNormalized(it) }

      // Add and remove plugins
      addBlockIdPlugins(ctx, appliedPluginsWithContext)
      removeBlockIdPlugins(blockPluginsContext)
    }

    blockStack.removeFirst()
  }

  @Throws(NonNormalizedScriptException::class)
  override fun exitPostfixUnaryExpression(ctx: PostfixUnaryExpressionContext) {
    if (blockStack.isNotEmpty()) return
    // Check if the script has been normalized
    PluginExtractor.extractFromScript(ctx)?.let { errorIfScriptNotNormalized(it) }
  }

  override fun enterStatement(ctx: StatementContext) {
    smartIndent.setIndent(ctx)
  }

  override fun exitScript(ctx: ScriptContext) {
    if (pluginsBlock == null) {
      addNewPluginBlock(ctx)
    }
  }

  /**
   * Add BlockId plugins to the existing plugin block
   */
  private fun addBlockIdPlugins(
    pluginsBlock: NamedBlockContext,
    appliedPluginsWithContext: Map<Plugin, ParserRuleContext>
  ) {
    // If there are no plugins to add or the block is not a plugins block, exit early
    if (pluginsToAdd.isEmpty() || !pluginsBlock.isPlugins) return

    // Generate the content to be added for the plugins
    val contentToAdd = pluginContentToAdd(appliedPluginsWithContext.keys.toList())

    // If no content needs to be added, exit early
    if (contentToAdd.isEmpty()) return

    val firstAppliedPlugin = appliedPluginsWithContext.values.takeIf { it.isNotEmpty() }?.first()
    val tokenToInsertBefore = if (firstAppliedPlugin != null) {
      // Insert before the first applied plugin
      Whitespace.getWhitespaceToLeft(tokens, firstAppliedPlugin.start)?.first()
    } else {
      // If plugin is empty, insert before the closing brace
      pluginsBlock.stop
    }

    rewriter.insertBefore(
      tokenToInsertBefore,
      contentToAdd.joinToString(separator = "\n") + "\n"
    )
  }

  private fun addNewPluginBlock(script: ScriptContext) {
    if (pluginsToAdd.isEmpty()) return

    val pluginsContentToAdd: List<String> = pluginContentToAdd(emptyList())
    val pluginBlockContent = buildString {
      appendLine("plugins {")
      pluginsContentToAdd.forEach { appendLine(it) }
      append("}")
    }

    rewriter.insertBefore(script.start, "$pluginBlockContent\n\n")
  }

  private fun removeBlockIdPlugins(ctx: List<ParserRuleContext>) {
    val blockIdPluginsToRemove = pluginsToRemove.map { pluginId ->
      Plugin(Plugin.Type.BLOCK_ID, pluginId)
    }
    val blockSimplePluginsToRemove = pluginsToRemove.map { pluginId ->
      Plugin(Plugin.Type.BLOCK_SIMPLE, pluginId)
    }
    val blockPluginsContextToRemove = ctx.filter {
      PluginExtractor.extractFromBlock(it) in blockIdPluginsToRemove + blockSimplePluginsToRemove
    }

    blockPluginsContextToRemove.forEach { line ->
      rewriter.delete(line.start, line.stop)

      rewriter.deleteWhitespaceToLeft(line.start)
      rewriter.deleteNewlineToRight(line.stop)
    }
  }

  private fun pluginContentToAdd(appliedPlugins: List<Plugin>): List<String> {
    return pluginsToAdd
      .map { pluginId -> Plugin(Plugin.Type.BLOCK_ID, pluginId) }
      .filter { pluginToAdd -> pluginToAdd !in appliedPlugins }
      .mapNotNull { plugin -> plugin.asIdString() }
      .map { "${smartIndent.getSmartIndent()}$it" }
  }

  private fun errorIfScriptNotNormalized(plugin: Plugin) {
    if (plugin.type != Plugin.Type.BLOCK_ID && plugin.type != Plugin.Type.BLOCK_SIMPLE) {
      throw NonNormalizedScriptException(
        "Unexpected plugin of type '${plugin.type}' and id ${plugin.id} detected. " +
          "Please normalize the script with PluginNormalizer.kt before attempting modifications."
      )
    }
  }

  public companion object {
    @Throws(IllegalArgumentException::class)
    public fun of(
      buildScript: Path,
      pluginsToAdd: Set<String>,
      pluginsToRemove: Set<String>
    ): PluginMutator {
      return of(Parser.readOnlyInputStream(buildScript), pluginsToAdd, pluginsToRemove)
    }

    @Throws(IllegalArgumentException::class)
    public fun of(
      buildScript: String,
      pluginsToAdd: Set<String>,
      pluginsToRemove: Set<String>
    ): PluginMutator {
      return of(buildScript.byteInputStream(), pluginsToAdd, pluginsToRemove)
    }

    @Throws(IllegalArgumentException::class)
    private fun of(
      buildScript: InputStream,
      pluginsToAdd: Set<String>,
      pluginsToRemove: Set<String>
    ): PluginMutator {
      val errorListener = CollectingErrorListener()

      val conflictingPlugins = pluginsToAdd.intersect(pluginsToRemove)
      if (conflictingPlugins.isNotEmpty()) {
        throw IllegalArgumentException("Conflicting plugins found: $conflictingPlugins")
      }

      return Parser(
        file = buildScript,
        errorListener = errorListener,
        listenerFactory = { _, tokens, _ ->
          PluginMutator(
            tokens = tokens,
            errorListener = errorListener,
            pluginsToAdd = pluginsToAdd,
            pluginsToRemove = pluginsToRemove
          )
        }
      ).listener()
    }
  }
}
