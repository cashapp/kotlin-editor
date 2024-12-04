package cash.grammar.kotlindsl.utils

import cash.grammar.kotlindsl.parse.SimpleErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer

public class CollectingErrorListener : SimpleErrorListener() {

  private val errorMessages = mutableListOf<String>()

  public fun getErrorMessages(): List<String> = errorMessages.toList()

  override fun syntaxError(
    recognizer: Recognizer<*, *>?,
    offendingSymbol: Any?,
    line: Int,
    charPositionInLine: Int,
    msg: String,
    e: RecognitionException?
  ) {
    errorMessages.add(msg)
  }
}
