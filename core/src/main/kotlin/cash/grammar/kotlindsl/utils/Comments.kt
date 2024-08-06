package cash.grammar.kotlindsl.utils

import com.squareup.cash.grammar.KotlinLexer
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token

public class Comments(
  private val tokens: CommonTokenStream,
  private val indent: String,
) {

  private var level = 0

  public fun onEnterBlock() {
    level++
  }

  public fun onExitBlock() {
    level--
  }

  public fun getCommentsToLeft(before: ParserRuleContext): String? {
    return getCommentsToLeft(before.start)
  }

  public fun getCommentsToLeft(before: Token): String? {
    var index = before.tokenIndex - 1
    if (index <= 0) return null

    var next = tokens.get(index)

    while (next != null && next.isWhitespace()) {
      next = tokens.get(--index)
    }

    if (next == null) return null

    val comments = ArrayDeque<String>()

    while (next != null) {
      if (next.isComment()) {
        comments.addFirst(next.text)
      } else if (next.isNotWhitespace()) {
        break
      }

      next = tokens.get(--index)
    }

    if (comments.isEmpty()) return null

    return comments.joinToString(separator = "\n") {
      "${indent.repeat(level)}$it"
    }
  }

  private fun Token.isWhitespace(): Boolean {
    return text.isBlank()
  }

  private fun Token.isNotWhitespace(): Boolean {
    return text.isNotBlank()
  }

  private fun Token.isComment(): Boolean {
    return type == KotlinLexer.LineComment || type == KotlinLexer.DelimitedComment
  }
}
