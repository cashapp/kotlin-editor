package cash.grammar.kotlindsl.parse

public class KotlinParseException private constructor(msg: String) : RuntimeException(msg) {

  public companion object {
    public fun withErrors(messages: List<String>): KotlinParseException {
      var i = 1
      val msg = messages.joinToString { "${i++}: $it" }
      return KotlinParseException(msg)
    }
  }
}
