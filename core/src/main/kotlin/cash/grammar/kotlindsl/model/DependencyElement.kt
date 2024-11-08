package cash.grammar.kotlindsl.model

import com.squareup.cash.grammar.KotlinParser.StatementContext

/**
 * Base class representing an element within the `dependencies` block.
 *
 * @property statement The context of the statement in the dependencies block.
 */
public sealed class DependencyElement(public open val statement: StatementContext)

/**
 * Represents a dependency declaration within the `dependencies` block.
 *
 * @property declaration The parsed dependency declaration.
 * @property statement The context of the statement in the dependencies block.
 */
public data class DependencyDeclarationElement(
  val declaration: DependencyDeclaration,
  override val statement: StatementContext
) : DependencyElement(statement)

/**
 * Represents a statement within the `dependencies` block that is **not** a dependency declaration.
 *
 * @property statement The context of the statement within the dependencies block.
 */
public data class NonDependencyDeclarationElement(override val statement: StatementContext) : DependencyElement(statement)
