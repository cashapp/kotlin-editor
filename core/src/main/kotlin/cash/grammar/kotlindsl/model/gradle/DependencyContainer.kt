package cash.grammar.kotlindsl.model.gradle

import cash.grammar.kotlindsl.model.*
import com.squareup.cash.grammar.KotlinParser.StatementContext

/**
 * A container for all the [Statements][com.squareup.cash.grammar.KotlinParser.StatementsContext] in
 * a `dependencies` block in a Gradle build script. These statements are an ordered (not sorted!)
 * list of statements, each classified as a [DependencyElement]
 *
 * Each statement in this container is classified as a [DependencyElement], which can represent either:
 * - A parsed [DependencyDeclaration][cash.grammar.kotlindsl.model.DependencyDeclaration] element, or
 * - A non-dependency declaration statement, retained as-is.
 */
public class DependencyContainer(
  /** The ordered list of [DependencyElement] instances, representing each classified statement within the `dependencies` block. */
  public val elements: List<DependencyElement>,
) {

  public fun getDependencyDeclarationsWithContext(): List<DependencyDeclarationElement> {
    return elements.filterIsInstance<DependencyDeclarationElement>()
  }

  public fun getDependencyDeclarations(): List<DependencyDeclaration> {
    return getDependencyDeclarationsWithContext().map { it.declaration }
  }

  /**
   * Get non-dependency declaration statements.
   *
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
    return elements.filterIsInstance<NonDependencyDeclarationElement>().map { it.statement }
  }

  internal companion object {
    val EMPTY = DependencyContainer(emptyList())
  }
}
