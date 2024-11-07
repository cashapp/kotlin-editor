package cash.grammar.kotlindsl.model.gradle

import cash.grammar.kotlindsl.model.DependencyDeclaration
import cash.grammar.kotlindsl.model.DependencyDeclarationWithContext
import com.squareup.cash.grammar.KotlinParser.StatementContext

/**
 * A container for all the [Statements][com.squareup.cash.grammar.KotlinParser.StatementsContext] in
 * a `dependencies` block in a Gradle build script. These statements are an ordered (not sorted!)
 * list of "raw" statements and modeled
 * [DependencyDeclarations][cash.grammar.kotlindsl.model.DependencyDeclaration].
 *
 * Rather than attempt to model everything that might possibly be found inside a build script, we
 * declare defeat on anything that isn't a standard dependency declaration and simply retain it
 * as-is.
 */
public class DependencyContainer(
  /** The raw, ordered, list of statements for more complex use-cases. */
  public val elements: List<Any>,
) {

  public fun getDependencyDeclarationsWithContext(): List<DependencyDeclarationWithContext> {
    return elements.filterIsInstance<DependencyDeclarationWithContext>()
  }

  public fun getDependencyDeclarations(): List<DependencyDeclaration> {
    val declarationsWithContext = getDependencyDeclarationsWithContext().map { it.declaration }
    return declarationsWithContext.ifEmpty {
      elements.filterIsInstance<DependencyDeclaration>()
    }
  }

  /**
   * Might include an [if-expression][com.squareup.cash.grammar.KotlinParser.IfExpressionContext] like
   * ```
   * if (functionReturningABoolean()) { ... }
   * ```
   * or a [property declaration][com.squareup.cash.grammar.KotlinParser.PropertyDeclarationContext] like
   * ```
   * val string = "a:complex:$value"
   * ```
   * or a common example of an expression in a dependencies block like
   * ```
   * add("extraImplementation", "com.foo:bar:1.0")
   * ```
   */
  public fun getStatements(): List<StatementContext> {
    return elements.filterIsInstance<StatementContext>()
  }

  internal companion object {
    val EMPTY = DependencyContainer(emptyList())
  }
}
