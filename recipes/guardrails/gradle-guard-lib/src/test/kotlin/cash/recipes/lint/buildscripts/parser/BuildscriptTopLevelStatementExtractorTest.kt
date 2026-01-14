package cash.recipes.lint.buildscripts.parser

import cash.recipes.lint.buildscripts.model.Position
import cash.recipes.lint.buildscripts.model.Statement
import cash.recipes.lint.buildscripts.utils.BuildScripts
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class BuildscriptTopLevelStatementExtractorTest {

  @Test
  fun `can extract top-level statements`() {
    // Given
    val linter = BuildscriptTopLevelStatementExtractor.of(BuildScripts.hasViolations1)

    // When
    val namedBlocks = linter.getStatements()

    // Then
    assertThat(namedBlocks).containsExactlyInAnyOrder(
      Statement.NamedBlock("plugins", Position(1, 0), Position(4, 0)),
      Statement.NamedBlock("dependencies", Position(6, 0), Position(22, 0)),
      Statement.NamedBlock("tasks", Position(24, 0), Position(28, 0)),
      Statement.Expression(
        "tasks.jar {",
        Position(line = 30, positionInLine = 0), stop = Position(line = 32, positionInLine = 0)
      ),
    )
  }

  @Test
  fun `can extract top-level statements in a more compelex script`() {
    // Given
    val linter = BuildscriptTopLevelStatementExtractor.of(BuildScripts.hasViolations3)

    // When
    val namedBlocks = linter.getStatements()

    // Then
    assertThat(namedBlocks).containsExactlyInAnyOrder(
      Statement.Expression("""apply(plugin = "foo")""", Position(1, 0), Position(1, 20)),
      Statement.Declaration("val bar = 1", Position(3, 0), Position(3, 10)),
      Statement.Declaration("public class MyTask : DefaultTask() {", Position(6, 0), Position(10, 0)),
    )
  }
}
