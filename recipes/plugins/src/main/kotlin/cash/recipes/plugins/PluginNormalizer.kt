package cash.recipes.plugins

import cash.grammar.kotlindsl.model.Plugin
import cash.grammar.kotlindsl.model.Plugin.Type
import cash.grammar.kotlindsl.parse.BuildScriptParseException
import cash.grammar.kotlindsl.parse.Parser
import cash.grammar.kotlindsl.parse.Rewriter
import cash.grammar.kotlindsl.utils.Blocks.isBuildscript
import cash.grammar.kotlindsl.utils.Blocks.isPlugins
import cash.grammar.kotlindsl.utils.CollectingErrorListener
import cash.grammar.kotlindsl.utils.Context.leafRule
import cash.grammar.kotlindsl.utils.PluginExtractor
import cash.grammar.kotlindsl.utils.SmartIndent
import cash.grammar.kotlindsl.utils.Whitespace
import cash.grammar.kotlindsl.utils.Whitespace.trimGently
import cash.grammar.utils.ifNotEmpty
import com.squareup.cash.grammar.KotlinLexer
import com.squareup.cash.grammar.KotlinParser
import com.squareup.cash.grammar.KotlinParser.*
import com.squareup.cash.grammar.KotlinParserBaseListener
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.TerminalNode
import java.io.InputStream
import java.nio.file.Path

/**
 * Finds plugins applied directly to this project and rewrites the build script to have a "normal"
 * form, maintaining application order. These are examples of plugins applied directly to this
 * project:
 * ```
 * plugins {
 *   id("foo)"
 *   kotlin("jvm")
 *   application
 *   `kotlin-dsl`
 * }
 *
 * apply(plugin = "bar")
 * ```
 * And this would be the normalized form for the above:
 * ```
 * plugins {
 *   id("foo")
 *   id("org.jetbrains.kotlin.jvm")
 *   id("application")
 *   id("kotlin-dsl")
 *   id("bar")
 * }
 * ```
 *
 * Note that plugins applied in `allprojects` blocks (for example) are ignored for purposes of this
 * class.
 */
public class PluginNormalizer private constructor(
  private val input: CharStream,
  private val tokens: CommonTokenStream,
  private val parser: KotlinParser,
  private val errorListener: CollectingErrorListener,
) : KotlinParserBaseListener() {

  private val rewriter = Rewriter(tokens)

  private val appliedPlugins = mutableListOf<Plugin>()
  private val blockStack = ArrayDeque<NamedBlockContext>()
  private var pluginsBlock: NamedBlockContext? = null

  private val smartIndent = SmartIndent(tokens)
  private var terminalNewlines = 0

  // Is this an empty script?
  private var empty = true

  @Throws(BuildScriptParseException::class)
  public fun rewritten(): String {
    errorListener.getErrorMessages().ifNotEmpty {
      throw BuildScriptParseException.withErrors(it)
    }

    return rewriter.text.trimGently(terminalNewlines)
  }

  override fun enterScript(ctx: ScriptContext) {
    terminalNewlines = Whitespace.countTerminalNewlines(ctx, tokens)
  }

  override fun enterStatement(ctx: StatementContext) {
    empty = false
    smartIndent.setIndent(ctx)
  }

  override fun enterNamedBlock(ctx: NamedBlockContext) {
    blockStack.addFirst(ctx)
  }

  override fun exitNamedBlock(ctx: NamedBlockContext) {
    if (ctx.isPlugins) {
      pluginsBlock = ctx
      findPlugins(ctx)
    }

    blockStack.removeFirst()
  }

  private fun findPlugins(ctx: NamedBlockContext) {
    require(ctx.isPlugins) { "Expected plugins block. Was '${ctx.name().text}'" }

    ctx.statements().statement().asSequence()
      .filterNot { it is TerminalNode }
      .map { it.leafRule() }
      .mapNotNull { PluginExtractor.extractFromBlock(it) }
      .forEach(appliedPlugins::add)
  }

  /**
   * Lots of code is a "postfix unary expression", e.g.
   * ```
   * apply(plugin = "kotlin")
   * ```
   * as well as
   * ```
   * "kotlin"
   * ```
   * This is the "closest" listener hook for getting the full `apply...` expression.
   */
  override fun exitPostfixUnaryExpression(ctx: PostfixUnaryExpressionContext) {
    if (blockStack.isNotEmpty()) return

    PluginExtractor.extractFromScript(ctx)?.let { plugin ->
      appliedPlugins.add(plugin)

      // Delete this plugin application & surrounding whitespace
      rewriter.deleteBlankSpaceToLeft(ctx.start)

      // TODO(tsr): this is problematic. Sometimes it deletes too much, other times not enough.
      rewriter.deleteBlankSpaceToRight(ctx.stop)

      rewriter.delete(ctx.start, ctx.stop)
    }
  }

  override fun exitScript(ctx: ScriptContext) {
    // Nothing to do if there are no applied plugins
    if (appliedPlugins.isEmpty()) return

    val indent = smartIndent.getSmartIndent()

    var content = buildString {
      appendLine("plugins {")
      appliedPlugins
        .map { plugin ->
          when (plugin.type) {
            Type.BLOCK_KOTLIN -> plugin.copy(
              type = Type.BLOCK_ID,
              id = "org.jetbrains.kotlin.${plugin.id}",
            )

            Type.APPLY -> {
              // https://github.com/search?type=code&q=org%3Asquareup+NOT+is%3Aarchived+%22apply%28plugin+%3D+%5C%22kotlin-%22
              when (plugin.id) {
                "kotlin" -> plugin.copy(
                  type = Type.BLOCK_ID,
                  id = "org.jetbrains.kotlin.jvm",
                )

                "kotlin-kapt" -> plugin.copy(
                  type = Type.BLOCK_ID,
                  id = "org.jetbrains.kotlin.kapt",
                )

                "kotlin-allopen" -> plugin.copy(
                  type = Type.BLOCK_ID,
                  id = "org.jetbrains.kotlin.plugin.allopen",
                )

                "kotlin-jpa" -> plugin.copy(
                  type = Type.BLOCK_ID,
                  id = "org.jetbrains.kotlin.plugin.jpa",
                )

                "kotlinx-serialization" -> plugin.copy(
                  type = Type.BLOCK_ID,
                  id = "org.jetbrains.kotlin.plugin.serialization",
                )

                else -> plugin
              }
            }

            else -> plugin
          }
        }
        .distinctBy { it.id }
        .forEach { plugin ->
          append(indent)

          if (plugin.type == Type.BLOCK_ALIAS) {
            append("alias(").append(plugin.id).append(")")
          } else {
            // this handles all other cases
            append("id(\"").append(plugin.id).append("\")")
          }

          plugin.version?.let { v ->
            append(" version $v")
          }

          if (!plugin.applied) {
            append(" apply false")
          }

          appendLine()
        }
      appendLine("}")
    }

    val pluginsBlock = pluginsBlock
    if (pluginsBlock != null) {
      if (isFollowedByTwoNewLines(pluginsBlock)) {
        // TODO(tsr): handle more complex kinds of newline
        content = content.removeSuffix("\n")
      }

      rewriter.replace(pluginsBlock.start, pluginsBlock.stop, content)
    } else {
      // There was no plugins block, so we must add one at top
      // Special handling in the case of a non-empty script
      if (!empty) {
        content = "$content\n"
      }

      // find the right place to insert a new block. It should be after any imports, if they exist,
      // and after a buildscript block, if it exists.

      val buildscriptStop = ctx.statement()
        ?.firstOrNull { statement -> statement.namedBlock()?.isBuildscript == true }
        ?.stop

      val insertAfter = buildscriptStop ?: ctx.importList().stop

      if (insertAfter != null) {
        // if we find a buildscript block or an import list, we insert _after_ that.
        rewriter.insertAfter(insertAfter, "\n\n$content")
      } else {
        // otherwise, we insert before (at the very stop of the script)
        rewriter.insertBefore(ctx.start, content)
      }
    }
  }

  private fun isFollowedByTwoNewLines(ctx: ParserRuleContext): Boolean {
    var next = ctx.stop.tokenIndex + 1

    if (next >= tokens.size()) return false
    var nextToken = tokens.get(next)
    if (nextToken.type != KotlinLexer.NL) return false

    next = nextToken.tokenIndex + 1
    if (next >= tokens.size()) return false

    nextToken = tokens.get(next)
    return nextToken.type == KotlinLexer.NL
  }

  public companion object {
    public fun of(buildScript: Path): PluginNormalizer {
      return of(Parser.readOnlyInputStream(buildScript))
    }

    public fun of(buildScript: String): PluginNormalizer {
      return of(buildScript.byteInputStream())
    }

    public fun of(buildScript: InputStream): PluginNormalizer {
      val errorListener = CollectingErrorListener()

      return Parser(
        file = buildScript,
        errorListener = errorListener,
        listenerFactory = { input, tokens, parser ->
          PluginNormalizer(
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
