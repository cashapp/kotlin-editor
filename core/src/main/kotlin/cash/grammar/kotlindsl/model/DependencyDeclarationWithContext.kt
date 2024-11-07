package cash.grammar.kotlindsl.model

import com.squareup.cash.grammar.KotlinParser.StatementContext

public data class DependencyDeclarationWithContext(
  val declaration: DependencyDeclaration,
  val statement: StatementContext
)