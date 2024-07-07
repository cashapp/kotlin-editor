package cash.grammar.kotlindsl.utils

import cash.grammar.kotlindsl.utils.Context.literalBoolean
import cash.grammar.kotlindsl.utils.Context.literalText
import com.squareup.cash.grammar.KotlinParser.InfixFunctionCallContext
import com.squareup.cash.grammar.KotlinParser.PostfixUnaryExpressionContext
import com.squareup.cash.grammar.KotlinParser.PostfixUnarySuffixContext
import com.squareup.cash.grammar.KotlinParser.RangeExpressionContext
import com.squareup.cash.grammar.KotlinParser.SimpleIdentifierContext
import org.antlr.v4.runtime.ParserRuleContext

/**
 * Extracts plugin configuration. For example:
 * ```
 * plugins {
 *   id("foo").version("x").apply(false)
 *   // or infix form
 *   id("foo") version "x" apply false
 * }
 * ```
 * Will provide access to `"x"` and `false` via [version] and [apply].
 */
internal class PluginConfigFinder private constructor(config: Config) {

  private interface Config {
    val version: String?
    val apply: Boolean
  }

  private class PostfixConfig(line: PostfixUnaryExpressionContext) : Config {

    private val config = line.postfixUnarySuffix()
      // this is the plugin ID, which we already have
      .drop(1)
      // the plugin config is chunked parts that each have two elements, (version, "v")
      // and (apply, <true|false>)
      .chunked(2)

    init {
      require(config.size <= 2) {
        "plugins can't have more than two sets of config (`version` and `apply`)"
      }
    }

    override val version: String? = configForName("version")?.let { c ->
      val arg = c[1]
        .callSuffix()
        ?.valueArguments()
        ?.getChild(1) as? ParserRuleContext
        ?: return@let null

      // if it's a string literal, wrap it in quotes. Otherwise, return the raw string
      // e.g. if the version is `libs.foo.get().version`.
      literalText(arg)?.let {
        "\"$it\""
      } ?: arg.text
    }

    override val apply: Boolean = configForName("apply")?.let { c ->
      val arg = c[1]
        .callSuffix()
        ?.valueArguments()
        ?.getChild(1) as? ParserRuleContext
        ?: return@let null

      literalBoolean(arg)
    } ?: true

    private fun configForName(name: String): List<PostfixUnarySuffixContext>? {
      return config.find { c ->
        if (c.size != 2) {
          false
        } else {
          name == c.first()?.navigationSuffix()?.simpleIdentifier()?.Identifier()?.text
        }
      }
    }
  }

  private class InfixConfig(
    private val line: InfixFunctionCallContext
  ) : Config {

    private val config = line.children.filterIsInstance<SimpleIdentifierContext>()
    private val versionConfig = config.firstOrNull { it.text == "version" }
    private val applyConfig = config.firstOrNull { it.text == "apply" }

    init {
      require(config.size <= 2) {
        "plugins can't have more than two sets of config (`version` and `apply`)"
      }
    }

    override val version: String? = versionConfig?.let { c ->
      val i = line.children.indexOf(c) + 1
      val value = line.getChild(i) as? RangeExpressionContext ?: return@let null
      literalText(value)?.let { "\"$it\"" } ?: value.text
    }

    override val apply: Boolean = applyConfig?.let { c ->
      val i = line.children.indexOf(c) + 1
      val value = line.getChild(i) as? RangeExpressionContext ?: return@let null
      literalBoolean(value) ?: return@let null
    } ?: true
  }

  companion object {
    fun of(line: PostfixUnaryExpressionContext): PluginConfigFinder {
      return PluginConfigFinder(PostfixConfig(line))
    }

    fun of(line: InfixFunctionCallContext): PluginConfigFinder {
      return PluginConfigFinder(InfixConfig(line))
    }
  }

  /** The plugin version. May be null. */
  val version: String? = config.version

  /** Whether the plugin is applied. Defaults to true. */
  val apply: Boolean = config.apply
}
