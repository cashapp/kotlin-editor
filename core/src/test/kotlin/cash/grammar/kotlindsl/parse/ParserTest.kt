package cash.grammar.kotlindsl.parse

import cash.grammar.kotlindsl.utils.test.TestErrorListener
import com.squareup.cash.grammar.KotlinParserBaseListener
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class ParserTest {

  @ParameterizedTest(name = "{0}")
  @MethodSource("multiDollarStrings")
  fun `can parse multi-dollar string interpolation prefixes`(source: String) {
    Parser(
      file = source,
      errorListener = TestErrorListener {
        throw RuntimeException("Syntax error: ${it?.message}", it)
      },
      listenerFactory = { _, _, _ -> KotlinParserBaseListener() },
    ).listener()
  }

  private companion object {
    @JvmStatic
    fun multiDollarStrings() = buildList {
      for (dollarCount in 2..4) {
        val interpolationPrefix = "\$".repeat(dollarCount)
        val literalPrefix = "\$".repeat(dollarCount - 1)
        val content = "literal ${literalPrefix}value and interpolated ${interpolationPrefix}value"
        add(named(
          "line string with $dollarCount-dollar prefix in default mode",
          "val value = $interpolationPrefix\"$content\"",
        ))
        add(named(
          "multiline string with $dollarCount-dollar prefix in default mode",
          "val value = $interpolationPrefix\"\"\"\n$content\n\"\"\"",
        ))
        add(named(
          "line string with $dollarCount-dollar prefix in inside mode",
          "consume($interpolationPrefix\"$content\")",
        ))
        add(named(
          "multiline string with $dollarCount-dollar prefix in inside mode",
          "consume($interpolationPrefix\"\"\"\n$content\n\"\"\")",
        ))
      }

      // https://github.com/square/gradle-dependencies-sorter/issues/154
      add(named(
        "multiline string in default mode from issue 154",
        issue154Source(),
      ))
    }

    private fun issue154Source(): String {
      val interpolationPrefix = "\$\$"
      val tripleQuote = "\"\"\""
      return """
        val generatedSource = $interpolationPrefix$tripleQuote
        |    /**
        |     * The full ${interpolationPrefix}description text as a string.
        |     */
        |    val text by lazy { checkNotNull(javaClass.getResource("/${interpolationPrefix}resourcePath/${'$'}id")).readText() }
        |}
        |
        |internal object ${interpolationPrefix}{className}Serializer : KSerializer<${interpolationPrefix}className> by ${interpolationPrefix}className.generatedSerializer() {
        |    override fun deserialize(decoder: Decoder): ${interpolationPrefix}className {
        |        require(decoder is YamlInput) {
        |            "Only YAML input is supported."
        |        }
        |
        |        val node = requireNotNull(decoder.node as? YamlScalar) {
        |            "Only scalar input is supported."
        |        }
        |
        |        return checkNotNull(${interpolationPrefix}className.forId(node.content)) {
        |            "No SPDX license found for ID '${'$'}{node.content}'."
        |        }
        |    }
        |}
        |
        $tripleQuote.trimMargin()
      """.trimIndent()
    }
  }
}
