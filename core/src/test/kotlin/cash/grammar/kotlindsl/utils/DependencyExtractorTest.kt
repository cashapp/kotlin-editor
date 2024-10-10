package cash.grammar.kotlindsl.utils

import cash.grammar.kotlindsl.model.DependencyDeclaration
import cash.grammar.kotlindsl.model.DependencyDeclaration.Capability
import cash.grammar.kotlindsl.model.DependencyDeclaration.Identifier.Companion.asSimpleIdentifier
import cash.grammar.kotlindsl.model.DependencyDeclaration.Type
import cash.grammar.kotlindsl.parse.Parser
import cash.grammar.kotlindsl.utils.test.TestErrorListener
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class DependencyExtractorTest {

  @ParameterizedTest(name = "{0}")
  @MethodSource("declarations")
  fun `can parse dependency declarations`(testCase: TestCase) {
    // Given
    val buildScript = """
      dependencies {
        ${testCase.fullText}
      }
    """.trimIndent()

    // When
    val scriptListener = Parser(
      file = buildScript,
      errorListener = TestErrorListener {
        throw RuntimeException("Syntax error: ${it?.message}", it)
      },
      listenerFactory = { input, tokens, _ -> TestListener(input, tokens) }
    ).listener()

    // Then
    assertThat(scriptListener.dependencyDeclarations).containsExactly(testCase.toDependencyDeclaration())
  }

  private companion object {
    // TODO(tsr): test for Capability.PLATFORM
    @JvmStatic fun declarations(): List<TestCase> = listOf(
      TestCase(
        displayName = "raw string coordinates",
        fullText = "api(\"g:a:v\")",
        configuration = "api",
        identifier = "\"g:a:v\"",
        capability = Capability.DEFAULT,
        type = Type.MODULE,
      ),
      TestCase(
        displayName = "version catalog accessor",
        fullText = "implementation(libs.gAV)",
        configuration = "implementation",
        identifier = "libs.gAV",
        capability = Capability.DEFAULT,
        type = Type.MODULE,
      ),
      TestCase(
        displayName = "project dependency",
        fullText = "textFixturesApi(project(\":has-test-fixtures\"))",
        configuration = "textFixturesApi",
        identifier = "\":has-test-fixtures\"",
        capability = Capability.DEFAULT,
        type = Type.PROJECT,
      ),
      TestCase(
        displayName = "testFixtures capability for project dependency",
        fullText = "testImplementation(testFixtures(project(\":has-test-fixtures\")))",
        configuration = "testImplementation",
        identifier = "\":has-test-fixtures\"",
        capability = Capability.TEST_FIXTURES,
        type = Type.PROJECT,
      ),
      TestCase(
        displayName = "gradleApi",
        fullText = "api(gradleApi())",
        configuration = "api",
        identifier = "gradleApi()",
        capability = Capability.DEFAULT,
        type = Type.GRADLE_DISTRIBUTION,
      ),
    )
  }

  internal class TestCase(
    val displayName: String,
    val fullText: String,
    val configuration: String,
    val identifier: String,
    val capability: Capability,
    val type: Type,
    val precedingComment: String? = null
  ) {
    override fun toString(): String = displayName

    fun toDependencyDeclaration() = DependencyDeclaration(
      configuration = configuration,
      identifier = identifier.asSimpleIdentifier()!!,
      capability = capability,
      type = type,
      fullText = fullText,
      precedingComment = precedingComment,
    )
  }
}
