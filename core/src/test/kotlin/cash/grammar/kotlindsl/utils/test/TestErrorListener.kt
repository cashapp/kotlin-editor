package cash.grammar.kotlindsl.utils.test

import cash.grammar.kotlindsl.parse.SimpleErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer

internal class TestErrorListener(
  private val onError: (RuntimeException?) -> Unit
) : SimpleErrorListener() {

  override fun syntaxError(
    recognizer: Recognizer<*, *>?,
    offendingSymbol: Any?,
    line: Int,
    charPositionInLine: Int,
    msg: String,
    e: RecognitionException?
  ) {
    onError.invoke(e)
  }
}