package cash.grammar.kotlindsl.parse

import cash.grammar.kotlindsl.utils.CollectingErrorListener
import cash.grammar.kotlindsl.utils.Whitespace
import cash.grammar.kotlindsl.utils.Whitespace.trimGently
import cash.grammar.utils.ifNotEmpty
import com.squareup.cash.grammar.KotlinParserBaseListener
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CommonTokenStream

/**
 * Subclass of [KotlinParserBaseListener] with an additional contract.
 */
public abstract class Mutator(
  protected val input: CharStream,
  protected val tokens: CommonTokenStream,
  protected val errorListener: CollectingErrorListener,
) : KotlinParserBaseListener() {

  protected val rewriter: Rewriter = Rewriter(tokens)
  protected val terminalNewlines: Int = Whitespace.countTerminalNewlines(tokens)
  protected val indent: String = Whitespace.computeIndent(tokens, input)

  /** Returns `true` if this mutator will make semantic changes to a file. */
  public abstract fun isChanged(): Boolean

  /**
   * Returns the new content of the file. Will contain semantic differences if and only if [isChanged] is true.
   */
  @Throws(KotlinParseException::class)
  public fun rewritten(): String {
    // TODO: check value of isChanged

    errorListener.getErrorMessages().ifNotEmpty {
      throw KotlinParseException.withErrors(it)
    }

    return rewriter.text.trimGently(terminalNewlines)
  }
}
