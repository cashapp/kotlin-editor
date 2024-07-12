package cash.recipes.repos

import cash.grammar.kotlindsl.parse.KotlinParseException
import cash.grammar.kotlindsl.parse.Parser
import cash.grammar.kotlindsl.utils.Blocks.isDependencyResolutionManagement
import cash.grammar.kotlindsl.utils.Blocks.isPlugins
import cash.grammar.kotlindsl.utils.CollectingErrorListener
import cash.grammar.kotlindsl.utils.SmartIndent
import cash.grammar.kotlindsl.utils.Whitespace
import cash.grammar.kotlindsl.utils.Whitespace.trimGently
import cash.grammar.utils.ifNotEmpty
import cash.recipes.repos.exception.AlreadyHasBlockException
import com.squareup.cash.grammar.KotlinParser
import com.squareup.cash.grammar.KotlinParser.NamedBlockContext
import com.squareup.cash.grammar.KotlinParser.ScriptContext
import com.squareup.cash.grammar.KotlinParser.StatementContext
import com.squareup.cash.grammar.KotlinParserBaseListener
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.TokenStreamRewriter
import java.io.InputStream
import java.nio.file.Path

/**
 * This will add a `dependencyResolutionManagement` block to a settings script that doesn't have it.
 *
 * ```
 * // settings.gradle.kts
 * dependencyResolutionManagement {
 *   repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
 *   repositories {
 *     maven(url = "...") // configurable
 *   }
 * }
 * ```
 */
public class DependencyResolutionManagementAdder private constructor(
  private val repos: List<String>,
  private val input: CharStream,
  private val tokens: CommonTokenStream,
  private val parser: KotlinParser,
  private val errorListener: CollectingErrorListener,
) : KotlinParserBaseListener() {

  private val smartIndent = SmartIndent(tokens)
  private var terminalNewlines = 0

  // Is this an empty script?
  private var empty = true

  private var alreadyHasBlock = false
  private var isDrmAdded = false

  private val rewriter = TokenStreamRewriter(tokens)

  @Throws(KotlinParseException::class, AlreadyHasBlockException::class)
  public fun rewritten(): String {
    errorListener.getErrorMessages().ifNotEmpty {
      throw KotlinParseException.withErrors(it)
    }

    if (alreadyHasBlock) {
      throw AlreadyHasBlockException(
        "Settings script already has a 'dependencyResolutionManagement' block"
      )
    }

    return rewriter.text.trimGently(terminalNewlines)
  }

  override fun enterScript(ctx: ScriptContext) {
    terminalNewlines = Whitespace.countTerminalNewlines(ctx, tokens)
  }

  override fun enterNamedBlock(ctx: NamedBlockContext) {
    if (!alreadyHasBlock && ctx.isDependencyResolutionManagement) {
      alreadyHasBlock = true
    }
  }

  // We'll make an effort to insert our new block _after_ the plugins block
  override fun exitNamedBlock(ctx: NamedBlockContext) {
    if (!isDrmAdded && ctx.isPlugins) {
      isDrmAdded = true
      rewriter.insertAfter(ctx.stop, getDependencyResolutionManagementText())
    }
  }

  // If there was no plugins block, add our block at the end of the script
  override fun exitScript(ctx: ScriptContext) {
    if (!isDrmAdded) {
      rewriter.insertAfter(ctx.stop, getDependencyResolutionManagementText())
    }
  }

  override fun enterStatement(ctx: StatementContext) {
    empty = false
    smartIndent.setIndent(ctx)
  }

  private fun getDependencyResolutionManagementText(): String {
    val indent = smartIndent.getSmartIndent()

    return buildString {
      // Special handling in the case of an empty script
      if (!empty) {
        appendLine()
        appendLine()
      }

      appendLine("dependencyResolutionManagement {")

      append(indent).appendLine("repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)")
      append(indent).appendLine("repositories {")

      repos.forEach {
        append(indent.repeat(2))
        appendLine(it)
      }

      append(indent).appendLine("}")
      append("}")
    }
  }

  public companion object {
    public fun of(
      settingsScript: Path,
      repos: List<String>,
    ): DependencyResolutionManagementAdder {
      return of(Parser.readOnlyInputStream(settingsScript), repos)
    }

    public fun of(
      settingsScript: String,
      repos: List<String>,
    ): DependencyResolutionManagementAdder {
      return of(settingsScript.byteInputStream(), repos)
    }

    public fun of(
      settingsScript: InputStream,
      repos: List<String>,
    ): DependencyResolutionManagementAdder {
      check(repos.isNotEmpty()) { "'repos' was empty. Expected at least one element." }

      val errorListener = CollectingErrorListener()

      return Parser(
        file = settingsScript,
        errorListener = errorListener,
        listenerFactory = { input, tokens, parser ->
          DependencyResolutionManagementAdder(
            repos = repos,
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
