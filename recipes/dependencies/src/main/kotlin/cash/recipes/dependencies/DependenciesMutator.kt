package cash.recipes.dependencies

import cash.grammar.kotlindsl.parse.Mutator
import cash.grammar.kotlindsl.parse.Parser
import cash.grammar.kotlindsl.utils.Blocks.isDependencies
import cash.grammar.kotlindsl.utils.CollectingErrorListener
import cash.grammar.kotlindsl.utils.Context.leafRule
import cash.grammar.kotlindsl.utils.DependencyExtractor
import cash.recipes.dependencies.transform.Transform
import com.squareup.cash.grammar.KotlinParser.NamedBlockContext
import com.squareup.cash.grammar.KotlinParser.PostfixUnaryExpressionContext
import com.squareup.cash.grammar.KotlinParser.ScriptContext
import com.squareup.cash.grammar.KotlinParser.StatementContext
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.Token
import java.io.InputStream
import java.nio.file.Path

/** Rewrites dependencies according to the provided [transforms]. */
public class DependenciesMutator private constructor(
  private val transforms: List<Transform>,
  input: CharStream,
  tokens: CommonTokenStream,
  errorListener: CollectingErrorListener,
) : Mutator(input, tokens, errorListener) {

  private val dependencyExtractor = DependencyExtractor(input, tokens, indent)
  private val usedTransforms = mutableListOf<Transform>()
  private var changes = false

  /**
   * Returns a list of all used transforms. Might be empty. This can be used to, for example, update a version catalog.
   */
  public fun usedTransforms(): List<Transform> = usedTransforms

  public override fun isChanged(): Boolean = changes

  override fun enterNamedBlock(ctx: NamedBlockContext) {
    dependencyExtractor.onEnterBlock()
  }

  override fun exitNamedBlock(ctx: NamedBlockContext) {
    if (isRealDependenciesBlock(ctx)) {
      onExitDependenciesBlock(ctx)
    }

    dependencyExtractor.onExitBlock()
  }

  private fun isRealDependenciesBlock(ctx: NamedBlockContext): Boolean {
    // parent is StatementContext. Parent of that should be ScriptContext
    // In contrast, with tasks.shadowJar { dependencies { ... } }, the parent.parent is StatementsContext
    if (ctx.parent.parent !is ScriptContext) return false

    return ctx.isDependencies
  }

  private fun onExitDependenciesBlock(ctx: NamedBlockContext) {
    val container = dependencyExtractor.collectDependencies(ctx)
    container.getDependencyDeclarationsWithContext()
      .mapNotNull { element ->
        val gav = element.declaration.identifier.path
        val identifier = buildString {
          append(gav.substringBeforeLast(':'))
          if (gav.startsWith("\"")) append("\"")
        }

        // E.g., "com.foo:bar:1.0" -> libs.fooBar OR
        //       "com.foo:bar"     -> libs.fooBar
        // We support the user passing in either the full GAV or just the identifier (without version)
        val transform = transforms.find { t -> t.from.matches(gav) }
          ?: transforms.find { t -> t.from.matches(identifier) }
        val newText = transform?.to?.render()

        if (newText != null) {
          // We'll return these entries later, so users can update their version catalogs as appropriate
          usedTransforms += transform
          element to newText
        } else {
          null
        }
      }
      .forEach { (element, newText) ->
        changes = true
        // postfix with parens because we took a shortcut with getStop() and cut it off
        rewriter.replace(getStart(element.statement), getStop(element.statement), "${newText})")
      }
  }

  /** Returns the token marking the start of the identifier (after the opening parentheses). */
  private fun getStart(ctx: StatementContext): Token {
    // statement -> postfixUnaryExpression -> postfixUnarySuffix -> callSuffix -> valueArguments -> valueArgument -> expression -> ... -> postfixUnaryExpression -> ... -> lineStringLiteral
    // statement -> postfixUnaryExpression -> postfixUnarySuffix -> callSuffix -> valueArguments -> valueArgument -> expression -> ... -> postfixUnaryExpression

    // This makes a lot of assumptions that I'm not sure are always valid. We do know that our ctx is for a dependency
    // declaration, though, which constrains the possibilities for the parse tree.
    return (ctx.leafRule() as PostfixUnaryExpressionContext)
      .postfixUnarySuffix()
      .single()
      .callSuffix()
      .valueArguments()
      .valueArgument()
      .single()
      .expression()
      .start
  }

  /** Returns the token marking the end of the declaration proper (before any trailing lambda). */
  private fun getStop(ctx: StatementContext): Token {
    val default = ctx.stop

    val leaf = ctx.leafRule()
    if (leaf !is PostfixUnaryExpressionContext) return default

    val postfix = leaf.postfixUnarySuffix().firstOrNull() ?: return default
    val preLambda = postfix.callSuffix().valueArguments()

    // we only want to replace everything BEFORE the trailing lambda (this includes the closing parentheses)
    return preLambda.stop
  }

  public companion object {
    /**
     * Returns a [DependenciesMutator], which eagerly parses [buildScript].
     *
     * @throws IllegalStateException if [DependencyExtractor] sees an expression it doesn't understand.
     * @throws IllegalArgumentException if [DependencyExtractor] sees an expression it doesn't understand.
     */
    @Throws(IllegalStateException::class, IllegalArgumentException::class)
    public fun of(
      buildScript: Path,
      transforms: List<Transform>,
    ): DependenciesMutator {
      return of(Parser.readOnlyInputStream(buildScript), transforms)
    }

    /**
     * Returns a [DependenciesMutator], which eagerly parses [buildScript].
     *
     * @throws IllegalStateException if [DependencyExtractor] sees an expression it doesn't understand.
     * @throws IllegalArgumentException if [DependencyExtractor] sees an expression it doesn't understand.
     */
    @Throws(IllegalStateException::class, IllegalArgumentException::class)
    public fun of(
      buildScript: String,
      transforms: List<Transform>,
    ): DependenciesMutator {
      return of(buildScript.byteInputStream(), transforms)
    }

    /**
     * Returns a [DependenciesMutator], which eagerly parses [buildScript].
     *
     * @throws IllegalStateException if [DependencyExtractor] sees an expression it doesn't understand.
     * @throws IllegalArgumentException if [DependencyExtractor] sees an expression it doesn't understand.
     */
    @Throws(IllegalStateException::class, IllegalArgumentException::class)
    private fun of(
      buildScript: InputStream,
      transforms: List<Transform>,
    ): DependenciesMutator {
      val errorListener = CollectingErrorListener()

      return Parser(
        file = buildScript,
        errorListener = errorListener,
        listenerFactory = { input, tokens, _ ->
          DependenciesMutator(
            transforms = transforms,
            input = input,
            tokens = tokens,
            errorListener = errorListener,
          )
        }
      ).listener()
    }
  }
}
