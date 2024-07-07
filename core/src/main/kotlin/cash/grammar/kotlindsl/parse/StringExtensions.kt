package cash.grammar.kotlindsl.parse

import com.squareup.cash.grammar.KotlinLexer
import com.squareup.cash.grammar.KotlinParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

/**
 * Parses the specified [String] and rewrites it using the specified [action]. Returns the
 * String object from the rewriter after the action has been applied.
 */
public fun String.rewrite(action: (KotlinParser.ScriptContext, Rewriter) -> Unit): String {
  val lexer = KotlinLexer(CharStreams.fromString(this))
  val tokens = CommonTokenStream(lexer)
  val parser = KotlinParser(tokens)
  val rewriter = Rewriter(tokens)

  action(parser.script(), rewriter)

  return rewriter.text
}

/**
 * Parses the specified [String] into a ScriptContext and processes it using the specified [action]. Returns the
 * result of the [action]. This is a convenience function for parsing and processing a script in one go.
 */
public fun <T> String.process(action: (KotlinParser.ScriptContext) -> T): T {
  val lexer = KotlinLexer(CharStreams.fromString(this))
  val tokens = CommonTokenStream(lexer)
  val parser = KotlinParser(tokens)

  return action(parser.script())
}