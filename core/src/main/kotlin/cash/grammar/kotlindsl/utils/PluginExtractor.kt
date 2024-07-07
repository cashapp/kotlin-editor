package cash.grammar.kotlindsl.utils

import cash.grammar.kotlindsl.model.Plugin
import cash.grammar.kotlindsl.model.Plugin.Type
import cash.grammar.kotlindsl.utils.Context.aliasText
import cash.grammar.kotlindsl.utils.Context.leafRule
import cash.grammar.kotlindsl.utils.Context.literalText
import com.squareup.cash.grammar.KotlinParser
import com.squareup.cash.grammar.KotlinParser.ExpressionContext
import com.squareup.cash.grammar.KotlinParser.InfixFunctionCallContext
import com.squareup.cash.grammar.KotlinParser.NamedBlockContext
import com.squareup.cash.grammar.KotlinParser.PostfixUnaryExpressionContext
import com.squareup.cash.grammar.KotlinParser.RangeExpressionContext
import com.squareup.cash.grammar.KotlinParser.SimpleIdentifierContext
import com.squareup.cash.grammar.KotlinParser.ValueArgumentContext
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.TerminalNode

/**
 * Extracts plugin from build scripts.
 *
 * nb for maintainers: I got to this by staring at the "Parse Tree" view in the ANTLR Preview tool
 * window until my eyes bled. Also note it's best to just assume everything is nullable, and also
 * that any list might be empty.
 */
public object PluginExtractor {

  private val pluginScriptBlocks = setOf(Blocks.ALLPROJECTS)

  /**
   * We can be in either the top level, or in an allprojects block at the top level.
   */
  public fun scriptLikeContext(blockStack: ArrayDeque<NamedBlockContext>): Boolean {
    val isAllprojectsBlock = blockStack.firstOrNull { namedBlock ->
      namedBlock.name().text in pluginScriptBlocks
    } != null

    return !(blockStack.size > 1 || (blockStack.size == 1 && !isAllprojectsBlock))
  }

  /**
   * Extracts a plugin ID and apply config from [line], from a `plugins` block, if there is one. May return null.
   * Must be called from inside a plugins block ([KotlinParser.NamedBlockContext]).
   *
   * Examples include
   * ```
   * plugins {
   *   id("foo")
   *   kotlin("jvm")
   *   application
   *   `kotlin-dsl`
   *   id("my-plugin") apply false
   *   id("my-other-plugin") version "x" apply false
   * }
   * ```
   */
  public fun extractFromBlock(line: ParserRuleContext): Plugin? {
    return when (line) {
      is SimpleIdentifierContext -> extractFromBlockSimple(line)

      is PostfixUnaryExpressionContext -> extractFromBlock(line)

      // For plugin with configuration e.g. version, apply
      is InfixFunctionCallContext -> extractFromBlockInfix(line)

      else -> null
    }
  }

  private fun extractFromBlockSimple(line: SimpleIdentifierContext): Plugin? {
    return if (line.text.startsWith("`")) {
      Plugin(
        type = Type.BLOCK_BACKTICK,
        id = line.text.removePrefix("`").removeSuffix("`")
      )
    } else {
      Plugin(
        type = Type.BLOCK_SIMPLE,
        id = line.text
      )
    }
  }

  private fun extractFromBlock(line: PostfixUnaryExpressionContext): Plugin? {
    // TODO(tsr): replace this with something?
    //if (line.postfixUnarySuffix().size != 1) return null

    // e.g., "id", "kotlin", or "alias"
    val type = line.primaryExpression()?.simpleIdentifier()?.text ?: return null

    val valueArgumentsCtx = line.postfixUnarySuffix(0)
      ?.callSuffix()
      ?.valueArguments()

    if (valueArgumentsCtx?.childCount != 3) return null
    val pluginIdRule = valueArgumentsCtx.getChild(1) as? ParserRuleContext ?: return null
    val pluginId = literalText(pluginIdRule)
      ?: aliasText(pluginIdRule)
      ?: return null

    // optional plugin config
    val config = PluginConfigFinder.of(line)

    return Plugin(
      type = Type.of(type),
      id = pluginId,
      version = config.version,
      applied = config.apply,
    )
  }

  private fun extractFromBlockInfix(line: InfixFunctionCallContext): Plugin? {
    val pluginArg = line.getChild(0) as? RangeExpressionContext ?: return null
    val plugin = when (val pluginArgLeaf = pluginArg.leafRule()) {
      is SimpleIdentifierContext -> extractFromBlockSimple(pluginArgLeaf)
      is PostfixUnaryExpressionContext -> extractFromBlock(pluginArgLeaf)
      else -> null
    } ?: return null

    // optional plugin config
    val config = PluginConfigFinder.of(line)

    return plugin.copy(
      version = config.version,
      applied = config.apply,
    )
  }

  /**
   * Extracts a plugin ID from [ctx] at the top level of a build script, if there is one. May return
   * null.
   *
   * Examples include
   * ```
   * apply(plugin = "kotlin")
   * apply(plugin = "com.github.johnrengelman.shadow")
   * apply(mapOf("plugin" to "foo"))
   * ```
   */
  public fun extractFromScript(ctx: PostfixUnaryExpressionContext): Plugin? {
    val enclosingBlockName = Blocks.enclosingNamedBlock(ctx)
    require(enclosingBlockName == null || enclosingBlockName in pluginScriptBlocks) {
      "Expected to be in the script context. Was in block named '$enclosingBlockName'"
    }

    if (ctx.primaryExpression().simpleIdentifier()?.text != "apply") return null
    if (ctx.postfixUnarySuffix().size != 1) return null

    // we might have an `apply(plugin = "...")` or an `apply(mapOf("plugin" to "..."))`
    val valueArgumentsCtx = ctx.postfixUnarySuffix(0)
      ?.callSuffix()
      ?.valueArguments()
      ?.valueArgument()
      ?: return null

    if (valueArgumentsCtx.size != 1) return null

    // seam: can be `apply(plugin = "...")` or `apply(mapOf("plugin" to "..."))`
    // example: 3 children: ["(", ["plugin", "=", "..."], ")"]
    // Note the 2nd child itself has three children
    if (valueArgumentsCtx[0].childCount == 3) {
      return findPluginFromAssignment(valueArgumentsCtx)
    } else if (valueArgumentsCtx[0].childCount == 1) {
      return findPluginFromMap(valueArgumentsCtx)
    }

    return null
  }

  /**
   * Returns [Plugin] from [valueArgumentsCtx], assuming
   * [argument.text][ExpressionContext.getText] == `plugin="foo"`. Returns `null` otherwise.
   */
  private fun findPluginFromAssignment(valueArgumentsCtx: List<ValueArgumentContext>): Plugin? {
    val arguments = valueArgumentsCtx[0]

    if ((arguments.getChild(0) as? SimpleIdentifierContext)?.text != "plugin") return null
    if ((arguments.getChild(1) as? TerminalNode)?.text != "=") return null

    val arg = arguments.getChild(2) as? ExpressionContext ?: return null
    val pluginId = literalText(arg) ?: return null

    return Plugin(
      type = Type.of("apply"),
      id = pluginId,
    )
  }

  /**
   * Returns [Plugin] from [valueArgumentsCtx], assuming
   * [argument.text][ExpressionContext.getText] == `"mapOf("plugin"to"foo")"`. Returns `null`
   * otherwise.
   */
  private fun findPluginFromMap(valueArgumentsCtx: List<ValueArgumentContext>): Plugin? {
    val arg = valueArgumentsCtx[0].getChild(0) as? ExpressionContext ?: return null
    val leaf = arg.leafRule() as? PostfixUnaryExpressionContext ?: return null

    if (leaf.primaryExpression()?.simpleIdentifier()?.text != "mapOf") return null
    if (leaf.postfixUnarySuffix().size != 1) return null

    val arguments = leaf.postfixUnarySuffix(0)
      ?.callSuffix()
      ?.valueArguments()

    if (arguments?.childCount != 3) return null

    val mapOf = ((arguments.getChild(1) as? ValueArgumentContext)
      ?.leafRule() as? InfixFunctionCallContext)
      ?: return null

    if (literalText(mapOf.rangeExpression(0)) != "plugin") return null
    val pluginId = literalText(mapOf.rangeExpression(1)) ?: return null

    return Plugin(
      type = Type.APPLY,
      id = pluginId,
    )
  }
}
