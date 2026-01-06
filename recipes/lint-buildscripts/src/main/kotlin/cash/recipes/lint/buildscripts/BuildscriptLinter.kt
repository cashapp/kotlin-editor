package cash.recipes.lint.buildscripts

import cash.grammar.kotlindsl.parse.Parser
import cash.grammar.kotlindsl.utils.CollectingErrorListener
import com.squareup.cash.grammar.KotlinParserBaseListener
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CommonTokenStream
import java.io.InputStream
import java.nio.file.Path

/**
 * TODO.
 */
public class BuildscriptLinter private constructor(
  private val input: CharStream,
  private val tokens: CommonTokenStream,
  private val errorListener: CollectingErrorListener,
) : KotlinParserBaseListener() {



  public companion object {
    /**
     * Returns a [BuildscriptLinter], which eagerly parses [buildScript].
     */
    public fun of(buildScript: Path): BuildscriptLinter {
      return of(Parser.readOnlyInputStream(buildScript))
    }

    /**
     * Returns a [BuildscriptLinter], which eagerly parses [buildScript].
     */
    public fun of(buildScript: String): BuildscriptLinter {
      return of(buildScript.byteInputStream())
    }

    /**
     * Returns a [BuildscriptLinter], which eagerly parses [buildScript].
     */
    private fun of(buildScript: InputStream): BuildscriptLinter {
      val errorListener = CollectingErrorListener()

      return Parser(
        file = buildScript,
        errorListener = errorListener,
        listenerFactory = { input, tokens, _ ->
          BuildscriptLinter(
            input = input,
            tokens = tokens,
            errorListener = errorListener,
          )
        }
      ).listener()
    }
  }
}