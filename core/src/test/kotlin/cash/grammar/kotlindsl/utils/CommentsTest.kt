package cash.grammar.kotlindsl.utils

import cash.grammar.kotlindsl.model.DependencyDeclaration
import cash.grammar.kotlindsl.model.DependencyDeclaration.Capability
import cash.grammar.kotlindsl.model.DependencyDeclaration.Identifier.Companion.asSimpleIdentifier
import cash.grammar.kotlindsl.model.DependencyDeclaration.Type
import cash.grammar.kotlindsl.parse.Parser
import cash.grammar.kotlindsl.utils.test.TestErrorListener
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class CommentsTest {

  @Test fun `can find comments`() {
    val buildScript =
      """
        dependencies {
          // This is a
          // comment
          implementation(libs.lib)
          /*
           * Here's a multiline comment.
           */
          implementation(deps.bar)
        }
      """.trimIndent()

    val scriptListener = Parser(
      file = buildScript,
      errorListener = TestErrorListener {
        throw RuntimeException("Syntax error: ${it?.message}", it)
      },
      listenerFactory = { input, tokens, _ -> TestListener(input, tokens) }
    ).listener()

    assertThat(scriptListener.dependencyDeclarations).containsExactly(
      DependencyDeclaration(
        configuration = "implementation",
        identifier = "libs.lib".asSimpleIdentifier()!!,
        capability = Capability.DEFAULT,
        type = Type.MODULE,
        fullText = "implementation(libs.lib)",
        precedingComment = "// This is a\n// comment",
      ),
      DependencyDeclaration(
        configuration = "implementation",
        identifier = "deps.bar".asSimpleIdentifier()!!,
        capability = Capability.DEFAULT,
        type = Type.MODULE,
        fullText = "implementation(deps.bar)",
        precedingComment = "/*\n   * Here's a multiline comment.\n   */",
      ),
    )
  }

  @Test fun `can find all comments in blocks`() {
    val buildScript =
      """
        dependencies {
          // This is a
          // comment
          implementation(libs.lib) // Inline comments
          /*
           * Here's a multiline comment.
           */
          implementation(deps.bar)
        }

        emptyBlock { }
      """.trimIndent()

    val scriptListener = Parser(
      file = buildScript,
      errorListener = TestErrorListener {
        throw RuntimeException("Syntax error: ${it?.message}", it)
      },
      listenerFactory = { input, tokens, _ -> TestListener(input, tokens) }
    ).listener()

    assertThat(scriptListener.commentTokens["dependencies"]?.map { it.text }).containsExactly(
      "// This is a",
      "// comment",
      "// Inline comments",
      "/*\n   * Here's a multiline comment.\n   */"
    )
    assertThat(scriptListener.commentTokens["emptyBlock"]?.map { it.text }).isEmpty()
  }

  @Test fun `handles dependencies at start of file without comments`() {
    val buildScript = "dependencies { implementation(libs.lib) }"

    val scriptListener = Parser(
      file = buildScript,
      errorListener = TestErrorListener {
        throw RuntimeException("Syntax error: ${it?.message}", it)
      },
      listenerFactory = { input, tokens, _ -> TestListener(input, tokens) }
    ).listener()

    assertThat(scriptListener.dependencyDeclarations).containsExactly(
      DependencyDeclaration(
        configuration = "implementation",
        identifier = "libs.lib".asSimpleIdentifier()!!,
        capability = Capability.DEFAULT,
        type = Type.MODULE,
        fullText = "implementation(libs.lib)",
        precedingComment = null,
      ),
    )
  }

  @Test fun `handles comment at start of file`() {
    val buildScript =
      """
        // Comment at start
        dependencies { implementation(libs.lib) }
      """.trimIndent()

    val scriptListener = Parser(
      file = buildScript,
      errorListener = TestErrorListener {
        throw RuntimeException("Syntax error: ${it?.message}", it)
      },
      listenerFactory = { input, tokens, _ -> TestListener(input, tokens) }
    ).listener()

    assertThat(scriptListener.dependencyDeclarations).containsExactly(
      DependencyDeclaration(
        configuration = "implementation",
        identifier = "libs.lib".asSimpleIdentifier()!!,
        capability = Capability.DEFAULT,
        type = Type.MODULE,
        fullText = "implementation(libs.lib)",
        precedingComment = null,
      ),
    )
  }
}
