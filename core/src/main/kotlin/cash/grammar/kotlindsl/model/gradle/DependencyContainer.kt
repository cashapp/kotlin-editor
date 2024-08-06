package cash.grammar.kotlindsl.model.gradle

import cash.grammar.kotlindsl.model.DependencyDeclaration

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
  private val statements: List<Any>,
) {

  public fun getDependencyDeclarations(): List<DependencyDeclaration> {
    return statements.filterIsInstance<DependencyDeclaration>()
  }

  public fun getNonDeclarations(): List<String> {
    return statements.filterIsInstance<String>()
  }

  internal companion object {
    val EMPTY = DependencyContainer(emptyList())
  }
}
