package cash.grammar.kotlindsl.parse

public class BuildScriptParseException private constructor(msg: String) : RuntimeException(msg) {

  public companion object {
    public fun withErrors(messages: List<String>): BuildScriptParseException {
      var i = 1
      val msg = messages.joinToString { "${i++}: $it" }
      return BuildScriptParseException(msg)
    }
  }
}
