package cash.grammar.kotlindsl.parse

import com.squareup.cash.grammar.KotlinLexer
import com.squareup.cash.grammar.KotlinParser
import com.squareup.cash.grammar.KotlinParserBaseListener
import com.squareup.cash.grammar.KotlinParserListener
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ParseTreeWalker
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * Create from an [InputStream], which is the primary type this parser operates on.
 */
public class Parser<T : KotlinParserListener>(
  /**
   * The source file to parse. This stream is immediately read and closed.
   */
  file: InputStream,

  /**
   * An error listener for use during source parsing.
   */
  private val errorListener: SimpleErrorListener = SimpleErrorListener(),

  /**
   * Specify the start rule. [KotlinParser.script] is used by default. The most common alternative
   * would be [KotlinParser.kotlinFile].
   */
  private val startRule: (KotlinParser) -> ParserRuleContext = { it.script() },

  /**
   * A factory for generating your custom [KotlinParserListener], typically a
   * [KotlinParserBaseListener].
   */
  private val listenerFactory: (CharStream, CommonTokenStream, KotlinParser) -> T,
) {

  /**
   * Create from a [Path], which is immediately converted into an [InputStream].
   */
  public constructor(
    file: Path,
    errorListener: SimpleErrorListener,
    listenerFactory: (CharStream, CommonTokenStream, KotlinParser) -> T,
  ) : this(
    file = Files.newInputStream(file, StandardOpenOption.READ),
    errorListener = errorListener,
    listenerFactory = listenerFactory,
  )

  /**
   * Create from a [String], which is immediately converted into an [InputStream].
   */
  public constructor(
    file: String,
    errorListener: SimpleErrorListener,
    listenerFactory: (CharStream, CommonTokenStream, KotlinParser) -> T,
  ) : this(
    file = file.byteInputStream(),
    errorListener = errorListener,
    listenerFactory = listenerFactory,
  )

  // Immediately read and close the stream
  private val input: CharStream = file.use {
    CharStreams.fromStream(it)
  }

  /**
   * Returns a new [KotlinParserListener], a subtype of
   * [ParseTreeListener][org.antlr.v4.runtime.tree.ParseTreeListener].
   *
   * @see <a href="https://github.com/antlr/antlr4/blob/master/doc/listeners.md">ANTLR Listeners</a>.
   */
  public fun listener(): T {
    val lexer = KotlinLexer(input)
    val tokens = CommonTokenStream(lexer)
    val parser = KotlinParser(tokens)

    // Remove default error listeners to prevent insane console output
    lexer.removeErrorListeners()
    parser.removeErrorListeners()

    lexer.addErrorListener(errorListener)
    parser.addErrorListener(errorListener)

    val walker = ParseTreeWalker()
    val tree = startRule(parser)
    val listener = listenerFactory(input, tokens, parser)
    walker.walk(listener, tree)

    return listener
  }

  public companion object {
    /** Creates an [InputStream] from [file] using [Files.newInputStream]. */
    public fun readOnlyInputStream(file: Path): InputStream {
      return Files.newInputStream(file, StandardOpenOption.READ)
    }
  }
}
