package cash.recipes.repos

import cash.grammar.kotlindsl.parse.BuildScriptParseException
import cash.grammar.kotlindsl.parse.Parser
import cash.grammar.kotlindsl.parse.Rewriter
import cash.grammar.kotlindsl.utils.Blocks
import cash.grammar.kotlindsl.utils.Blocks.isBuildscript
import cash.grammar.kotlindsl.utils.Blocks.isRepositories
import cash.grammar.kotlindsl.utils.CollectingErrorListener
import cash.grammar.kotlindsl.utils.Whitespace
import cash.grammar.kotlindsl.utils.Whitespace.trimGently
import cash.grammar.utils.ifNotEmpty
import com.squareup.cash.grammar.KotlinParser
import com.squareup.cash.grammar.KotlinParser.NamedBlockContext
import com.squareup.cash.grammar.KotlinParser.ScriptContext
import com.squareup.cash.grammar.KotlinParserBaseListener
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CommonTokenStream
import java.io.InputStream
import java.nio.file.Path

/**
 * Deletes `repositories {}` blocks from build scripts.
 *
 * Given this:
 * ```
 * // build.gradle.kts
 * buildscript {
 *   repositories { ... }
 *   ...
 * }
 *
 * repositories { ... }
 *
 * subprojects {
 *   ...
 *   buildscript {
 *     repositories { ... }
 *   }
 *   repositories { ... }
 * }
 * ```
 *
 * Transform into:
 * ```
 * buildscript {
 *   repositories { ... }
 *   ...
 * }
 *
 * subprojects {
 *   ...
 * }
 * ```
 *
 * That is, keep only the `buildscript.repositories { ... }` stanza for [buildScript][input] itself.
 */
public class RepositoriesDeleter private constructor(
  private val input: CharStream,
  private val tokens: CommonTokenStream,
  private val parser: KotlinParser,
  private val errorListener: CollectingErrorListener,
) : KotlinParserBaseListener() {

  private val rewriter = Rewriter(tokens)
  private var terminalNewlines = 0
  private val blockStack = ArrayDeque<NamedBlockContext>()

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

  override fun enterNamedBlock(ctx: NamedBlockContext) {
    blockStack.addFirst(ctx)
  }

  override fun exitNamedBlock(ctx: NamedBlockContext) {
    if (!isInBuildscriptBlock() && ctx.isRepositories) {
      // Delete block
      Blocks.getOutermostBlock(blockStack)?.let { block ->
        // Delete preceding whitespace
        rewriter.deleteBlankSpaceToLeft(block.start)

        rewriter.delete(block.start, block.stop)
      }
    }

    // Must be last!
    blockStack.removeFirst()
  }

  /**
   * Returns true if the outermost block in the current context is `buildscript {}`.
   */
  private fun isInBuildscriptBlock(): Boolean {
    return blockStack.isNotEmpty() && blockStack.last().isBuildscript
  }

  @Suppress("MemberVisibilityCanBePrivate")
  public companion object {
    public fun of(buildScript: Path): RepositoriesDeleter {
      return of(Parser.readOnlyInputStream(buildScript))
    }

    public fun of(buildScript: String): RepositoriesDeleter {
      return of(buildScript.byteInputStream())
    }

    public fun of(buildScript: InputStream): RepositoriesDeleter {
      val errorListener = CollectingErrorListener()

      return Parser(
        file = buildScript,
        errorListener = errorListener,
        listenerFactory = { input, tokens, parser ->
          RepositoriesDeleter(
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
