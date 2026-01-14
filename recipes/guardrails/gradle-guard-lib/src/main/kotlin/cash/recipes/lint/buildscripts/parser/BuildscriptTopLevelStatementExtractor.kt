package cash.recipes.lint.buildscripts.parser

import cash.grammar.kotlindsl.parse.Parser
import cash.grammar.kotlindsl.utils.CollectingErrorListener
import cash.grammar.kotlindsl.utils.Context.fullText
import cash.grammar.kotlindsl.utils.Context.lastChildOrThrow
import cash.grammar.kotlindsl.utils.Context.leafRule
import cash.grammar.kotlindsl.utils.Context.singleChildOrThrow
import cash.grammar.kotlindsl.utils.Statements
import cash.recipes.lint.buildscripts.model.Position
import cash.recipes.lint.buildscripts.model.Statement
import com.squareup.cash.grammar.KotlinParser
import com.squareup.cash.grammar.KotlinParserBaseListener
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import java.io.InputStream
import java.nio.file.Path

/** Extracts all the top-level statements from a buildscript. */
internal class BuildscriptTopLevelStatementExtractor private constructor(
  private val input: CharStream,
  private val tokens: CommonTokenStream,
  private val errorListener: CollectingErrorListener,
) : KotlinParserBaseListener() {

  private val statements = mutableListOf<Statement>()
  private val statementsHelper = Statements()

  fun getStatements(): List<Statement> = statements

  override fun enterStatement(ctx: KotlinParser.StatementContext) {
    statementsHelper.onEnterStatement()

    if (!statementsHelper.isTopLevel()) {
      return
    }

    val leaf = ctx.leafRule()
    if (leaf is KotlinParser.NamedBlockContext) {
      statements.add(leaf.toStatement())
      return
    }

    // The statement can either have a single child or have any number of labels|annotations followed by a statement.
    val stmt = if (ctx.childCount > 1) {
      val child = ctx.lastChildOrThrow()
      child.firstLine()?.let { child.toStatement(it) }
    } else {
      leaf.firstLine()?.let { ctx.singleChildOrThrow().toStatement(it) }
    }

    stmt?.let { statements.add(it) }
  }

  override fun exitStatement(ctx: KotlinParser.StatementContext?) {
    statementsHelper.onExitStatement()
  }

  /** Get the first line of a statement. E.g., "tasks.jar {" */
  private fun ParserRuleContext.firstLine(): String? {
    return fullText(input)?.lineSequence()?.firstOrNull { it.isNotBlank() }
  }

  private fun ParserRuleContext.toStatement(text: String): Statement? {
    return when (this) {
      is KotlinParser.AssignmentContext -> Statement.Assignment(text, start.toPosition(), stop.toPosition())
      is KotlinParser.DeclarationContext -> Statement.Declaration(text, start.toPosition(), stop.toPosition())
      is KotlinParser.ExpressionContext -> Statement.Expression(text, start.toPosition(), stop.toPosition())
      is KotlinParser.LoopStatementContext -> Statement.Loop(text, start.toPosition(), stop.toPosition())

      else -> {
        println("Unknown context: ${this.javaClass.simpleName}. Expected one of [AssignmentContext, DeclarationContext, ExpressionContext, LoopStatementContext].")
        null
      }
    }
  }

  private fun KotlinParser.NamedBlockContext.toStatement(): Statement {
    return Statement.NamedBlock(
      name = name().Identifier().text,
      start = start.toPosition(),
      stop = stop.toPosition(),
    )
  }

  private fun Token.toPosition(): Position {
    return Position(line = line, positionInLine = charPositionInLine)
  }

  companion object {
    /** Returns a [BuildscriptTopLevelStatementExtractor], which eagerly parses [buildScript]. */
    fun of(buildScript: Path): BuildscriptTopLevelStatementExtractor {
      return of(Parser.readOnlyInputStream(buildScript))
    }

    /** Returns a [BuildscriptTopLevelStatementExtractor], which eagerly parses [buildScript]. */
    fun of(buildScript: String): BuildscriptTopLevelStatementExtractor {
      return of(buildScript.byteInputStream())
    }

    /** Returns a [BuildscriptTopLevelStatementExtractor], which eagerly parses [buildScript]. */
    private fun of(buildScript: InputStream): BuildscriptTopLevelStatementExtractor {
      val errorListener = CollectingErrorListener()

      return Parser(
        file = buildScript,
        errorListener = errorListener,
        listenerFactory = { input, tokens, _ ->
          BuildscriptTopLevelStatementExtractor(
            input = input,
            tokens = tokens,
            errorListener = errorListener,
          )
        }
      ).listener()
    }
  }
}
