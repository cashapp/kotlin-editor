package cash.grammar.kotlindsl.utils

import cash.grammar.kotlindsl.parse.KotlinParseException
import cash.grammar.kotlindsl.parse.Parser
import cash.grammar.kotlindsl.parse.Rewriter
import cash.grammar.kotlindsl.utils.Context.leafRule
import cash.grammar.kotlindsl.utils.Whitespace.trimGently
import cash.grammar.utils.ifNotEmpty
import com.squareup.cash.grammar.KotlinParser
import com.squareup.cash.grammar.KotlinParserBaseListener
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.Token
import java.io.InputStream
import java.nio.file.Path

/**
 * Removes comments from a specified block in a build script.
 *
 * Example:
 * ```
 * dependencies {
 *     /* This is a block comment
 *     that spans multiple lines */
 *     implementation("org.jetbrains.kotlin:kotlin-stdlib") // This is an inline comment
 *     // This is a single-line comment
 *     testImplementation("org.junit.jupiter:junit-jupiter")
 * }
 * ```
 *
 * The above script would be rewritten to:
 * ```
 * dependencies {
 *     implementation("org.jetbrains.kotlin:kotlin-stdlib")
 *     testImplementation("org.junit.jupiter:junit-jupiter")
 * }
 * ```
 */
public class CommentsInBlockRemover private constructor(
  private val input: CharStream,
  private val tokens: CommonTokenStream,
  private val errorListener: CollectingErrorListener,
  private val blockName: String,
) : KotlinParserBaseListener() {
  private var terminalNewlines = 0
  private val rewriter = Rewriter(tokens)
  private val indent = Whitespace.computeIndent(tokens, input)
  private val comments = Comments(tokens, indent)

  @Throws(KotlinParseException::class)
  public fun rewritten(): String {
    errorListener.getErrorMessages().ifNotEmpty {
      throw KotlinParseException.withErrors(it)
    }

    return rewriter.text.trimGently(terminalNewlines)
  }

  override fun exitNamedBlock(ctx: KotlinParser.NamedBlockContext) {
    if (ctx.name().text == blockName) {
      // Delete inline comments (a comment after a statement)
      val allInlineComments = mutableListOf<Token>()
      ctx.statements().statement().forEach {
        val leafRule = it.leafRule()
        val inlineComments = rewriter.deleteCommentsAndBlankSpaceToRight(leafRule.stop).orEmpty()
        allInlineComments += inlineComments
      }

      val nonInlineComments = comments.getCommentsInBlock(ctx).subtract(allInlineComments)
      nonInlineComments.forEach { token ->
        rewriter.deleteWhitespaceToLeft(token)
        rewriter.deleteNewlineToRight(token)
        rewriter.delete(token)
      }
    }
  }

  public companion object {
    public fun of(
      buildScript: Path,
      blockName: String,
    ): CommentsInBlockRemover {
      return of(Parser.readOnlyInputStream(buildScript), blockName)
    }

    public fun of(
      buildScript: String,
      blockName: String,
    ): CommentsInBlockRemover {
      return of(buildScript.byteInputStream(), blockName)
    }

    private fun of(
      buildScript: InputStream,
      blockName: String,
    ): CommentsInBlockRemover {
      val errorListener = CollectingErrorListener()

      return Parser(
        file = buildScript,
        errorListener = errorListener,
        listenerFactory = { input, tokens, _ ->
          CommentsInBlockRemover(
            input = input,
            tokens = tokens,
            errorListener = errorListener,
            blockName = blockName,
          )
        },
      ).listener()
    }
  }
}