package cash.recipes.dependencies

import cash.grammar.kotlindsl.model.DependencyDeclaration
import cash.grammar.kotlindsl.parse.KotlinParseException
import cash.grammar.kotlindsl.parse.Parser
import cash.grammar.kotlindsl.parse.Rewriter
import cash.grammar.kotlindsl.utils.Blocks.isBuildscript
import cash.grammar.kotlindsl.utils.Blocks.isDependencies
import cash.grammar.kotlindsl.utils.CollectingErrorListener
import cash.grammar.kotlindsl.utils.Context.leafRule
import cash.grammar.kotlindsl.utils.DependencyExtractor
import cash.grammar.kotlindsl.utils.Whitespace
import cash.grammar.kotlindsl.utils.Whitespace.trimGently
import cash.grammar.utils.ifNotEmpty
import com.squareup.cash.grammar.KotlinParser.NamedBlockContext
import com.squareup.cash.grammar.KotlinParser.PostfixUnaryExpressionContext
import com.squareup.cash.grammar.KotlinParser.StatementContext
import com.squareup.cash.grammar.KotlinParserBaseListener
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.Token
import java.io.InputStream
import java.nio.file.Path

public class DependenciesSimplifier private constructor(
  private val input: CharStream,
  private val tokens: CommonTokenStream,
  private val errorListener: CollectingErrorListener,
) : KotlinParserBaseListener() {

  private val rewriter = Rewriter(tokens)
  private val indent = Whitespace.computeIndent(tokens, input)
  private val terminalNewlines = Whitespace.countTerminalNewlines(tokens)
  private val dependencyExtractor = DependencyExtractor(input, tokens, indent)

  private var changes = false
  private var isInBuildscriptBlock = false

  public fun isChanged(): Boolean = changes

  @Throws(KotlinParseException::class)
  public fun rewritten(): String {
    errorListener.getErrorMessages().ifNotEmpty {
      throw KotlinParseException.withErrors(it)
    }

    return rewriter.text.trimGently(terminalNewlines)
  }

  override fun enterNamedBlock(ctx: NamedBlockContext) {
    dependencyExtractor.onEnterBlock()

    if (ctx.isBuildscript) {
      isInBuildscriptBlock = true
    }
  }

  override fun exitNamedBlock(ctx: NamedBlockContext) {
    if (ctx.isDependencies && !isInBuildscriptBlock) {
      onExitDependenciesBlock(ctx)
    }

    if (ctx.isBuildscript) {
      isInBuildscriptBlock = false
    }

    dependencyExtractor.onExitBlock()
  }

  private fun onExitDependenciesBlock(ctx: NamedBlockContext) {
    val container = dependencyExtractor.collectDependencies(ctx)
    container.getDependencyDeclarationsWithContext()
      // we only care about complex declarations. We will rewrite this in simplified form
      .filter { it.declaration.isComplex }
      .forEach { element ->
        val declaration = element.declaration
        val elementCtx = element.statement

        val newText = simplify(declaration)

        if (newText != null) {
          changes = true
          rewriter.replace(elementCtx.start, getStop(elementCtx), newText)
        }
      }
  }

  private fun getStop(ctx: StatementContext): Token {
    val default = ctx.stop

    val leaf = ctx.leafRule()
    if (leaf !is PostfixUnaryExpressionContext) return default

    val postfix = leaf.postfixUnarySuffix().firstOrNull() ?: return default
    val preLambda = postfix.callSuffix().valueArguments()

    // we only want to replace everything BEFORE the trailing lambda
    return preLambda.stop
  }

  private fun simplify(declaration: DependencyDeclaration): String? {
    require(declaration.isComplex) { "Expected complex declaration, was $declaration" }

    // TODO(tsr): For now, ignore those that have ext, classifier, producerConfiguration
    if (declaration.ext != null || declaration.classifier != null || declaration.producerConfiguration != null) {
      return null
    }

    return buildString {
      append(declaration.configuration)
      append("(")
      append(declaration.identifier)
      append(")")
    }
  }

  public companion object {
    public fun of(buildScript: Path): DependenciesSimplifier {
      return of(Parser.readOnlyInputStream(buildScript))
    }

    public fun of(buildScript: String): DependenciesSimplifier {
      return of(buildScript.byteInputStream())
    }

    private fun of(buildScript: InputStream): DependenciesSimplifier {
      val errorListener = CollectingErrorListener()

      return Parser(
        file = buildScript,
        errorListener = errorListener,
        listenerFactory = { input, tokens, _ ->
          DependenciesSimplifier(
            input = input,
            tokens = tokens,
            errorListener = errorListener,
          )
        }
      ).listener()
    }
  }
}
