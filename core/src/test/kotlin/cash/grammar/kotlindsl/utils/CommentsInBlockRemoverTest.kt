package cash.grammar.kotlindsl.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CommentsInBlockRemoverTest {
  @Test
  fun `remove all comments in the given block`() {
    // Given
    val buildScript = """
      |dependencies {
      |  /* This is a block comment
      |  that spans multiple lines */
      |  implementation("org.jetbrains.kotlin:kotlin-stdlib") // This is an line comment
      |  // This is a single-line comment
      |  testImplementation("org.junit.jupiter:junit-jupiter")
      |  // This is another single-line comment
      |}
      |
      |// This is project bar
      |project.name = bar 
      |
      |otherBlock {
      |  // More comments 
      |}
    """.trimMargin()

    // When
    val rewrittenBuildScript = CommentsInBlockRemover.of(buildScript, "dependencies").rewritten()

    // Then
    assertThat(rewrittenBuildScript).isEqualTo("""
      |dependencies {
      |  implementation("org.jetbrains.kotlin:kotlin-stdlib")
      |  testImplementation("org.junit.jupiter:junit-jupiter")
      |}
      |
      |// This is project bar
      |project.name = bar 
      |
      |otherBlock {
      |  // More comments 
      |}
    """.trimMargin())
  }
}