package cash.grammar.kotlindsl.parse

import org.antlr.v4.runtime.ANTLRErrorListener
import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.atn.ATNConfigSet
import org.antlr.v4.runtime.dfa.DFA
import java.util.BitSet

/**
 * An [ANTLRErrorListener] that ignores all errors. See also
 * [CollectingErrorListener][cash.grammar.kotlindsl.utils.CollectingErrorListener].
 */
public open class SimpleErrorListener : ANTLRErrorListener {

  override fun syntaxError(
    recognizer: Recognizer<*, *>?,
    offendingSymbol: Any?,
    line: Int,
    charPositionInLine: Int,
    msg: String,
    e: RecognitionException?
  ) {
  }

  override fun reportAmbiguity(
    recognizer: Parser,
    dfa: DFA,
    startIndex: Int,
    stopIndex: Int,
    exact: Boolean,
    ambigAlts: BitSet,
    configs: ATNConfigSet
  ) {
  }

  override fun reportAttemptingFullContext(
    recognizer: Parser,
    dfa: DFA?,
    startIndex: Int,
    stopIndex: Int,
    conflictingAlts: BitSet,
    configs: ATNConfigSet
  ) {
  }

  override fun reportContextSensitivity(
    recognizer: Parser,
    dfa: DFA?,
    startIndex: Int,
    stopIndex: Int,
    prediction: Int,
    configs: ATNConfigSet
  ) {
  }
}
