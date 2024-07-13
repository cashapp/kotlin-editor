package cash.grammar.kotlindsl.utils

import cash.grammar.kotlindsl.model.RemovableBlock
import cash.grammar.kotlindsl.parse.KotlinParseException
import cash.grammar.kotlindsl.parse.Parser
import cash.grammar.kotlindsl.parse.Rewriter
import cash.grammar.kotlindsl.utils.Whitespace.trimGently
import cash.grammar.utils.ifNotEmpty
import com.squareup.cash.grammar.KotlinParser
import com.squareup.cash.grammar.KotlinParserBaseListener
import org.antlr.v4.runtime.CommonTokenStream
import java.io.InputStream
import java.nio.file.Path

/**
 * A utility class for removing specified blocks from a Gradle Kotlin build script.
 *
 * @property tokens The token stream for the parser.
 * @property errorListener The listener that collects parsing errors.
 * @property blocksToRemove The set of [RemovableBlock]s to be removed from the build script
 */
public class BlockRemover private constructor(
  private val tokens: CommonTokenStream,
  private val errorListener: CollectingErrorListener,
  private val blocksToRemove: Set<RemovableBlock>
) : KotlinParserBaseListener() {

  private val rewriter = Rewriter(tokens)
  private val terminalNewlines = Whitespace.countTerminalNewlines(tokens)

  @Throws(KotlinParseException::class)
  public fun rewritten(): String {
    errorListener.getErrorMessages().ifNotEmpty {
      throw KotlinParseException.withErrors(it)
    }

    return rewriter.text.trimGently(terminalNewlines)
  }

  override fun exitNamedBlock(ctx: KotlinParser.NamedBlockContext) {
    val simpleBlocks = blocksToRemove.filterIsInstance<RemovableBlock.SimpleBlock>()

    if (ctx.name().text in simpleBlocks.map { it.name }) {
      // delete whole block and spaces around it
      rewriter.delete(ctx.start, ctx.stop)
      rewriter.deleteWhitespaceToLeft(ctx.start)
      rewriter.deleteNewlineToRight(ctx.stop)
    }
  }

  override fun exitPostfixUnaryExpression(ctx: KotlinParser.PostfixUnaryExpressionContext) {
    val tasksWithTypeToRemove =
      blocksToRemove.filterIsInstance<RemovableBlock.TaskWithTypeBlock>().map { it.type }.toSet()
    val inTasksWithType =
      ctx.primaryExpression()?.simpleIdentifier()?.text == "tasks" && ctx.postfixUnarySuffix(0)
        ?.navigationSuffix()?.simpleIdentifier()?.text == "withType"

    if (inTasksWithType) {
      removeWithType(ctx, tasksWithTypeToRemove)
    }
  }

  private fun removeWithType(
    ctx: KotlinParser.PostfixUnaryExpressionContext,
    typeNames: Set<String>
  ) {
    // tasks.withType<TypeName>
    val kotlinDSLType =
      ctx.postfixUnarySuffix(1)?.typeArguments()?.typeProjection()?.singleOrNull()?.type()?.text
    // tasks.withType(TypeName)
    val groovyDSLType = ctx.postfixUnarySuffix(1)?.callSuffix()?.valueArguments()?.valueArgument()
      ?.singleOrNull()?.text

    if (kotlinDSLType in typeNames || groovyDSLType in typeNames) {
      rewriter.delete(ctx.start, ctx.stop)
      rewriter.deleteWhitespaceToLeft(ctx.start)
      rewriter.deleteNewlineToRight(ctx.stop)
    }
  }

  public companion object {
    public fun of(
      buildScript: Path,
      blocksToRemove: Set<RemovableBlock>
    ): BlockRemover {
      return of(Parser.readOnlyInputStream(buildScript), blocksToRemove)
    }

    public fun of(
      buildScript: String,
      blocksToRemove: Set<RemovableBlock>
    ): BlockRemover {
      return of(buildScript.byteInputStream(), blocksToRemove)
    }

    private fun of(
      buildScript: InputStream,
      blocksToRemove: Set<RemovableBlock>
    ): BlockRemover {
      val errorListener = CollectingErrorListener()

      return Parser(
        file = buildScript,
        errorListener = errorListener,
        listenerFactory = { _, tokens, _ ->
          BlockRemover(
            tokens = tokens,
            errorListener = errorListener,
            blocksToRemove = blocksToRemove
          )
        }
      ).listener()
    }
  }
}

