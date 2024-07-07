package cash.grammar.kotlindsl.utils

import cash.grammar.kotlindsl.model.RemovableBlock.SimpleBlock
import cash.grammar.kotlindsl.model.RemovableBlock.TaskWithTypeBlock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BlockRemoverTest {
  @Test
  fun `can remove SimpleBlock`() {
    val buildScript =
      """
        |plugins {
        |  application
        |}
        |
        |kotlinter {
        |  disabledRules = arrayOf("max-line-length") // Too many failures at the moment that would need manual intervention
        |}
        |
        |polyrepo {
        |  publishToArtifactory = true // cash.server.protos does not publish by default
        |  shortName = "foo"
        |}
        |
      """.trimMargin()

    // When...
    val blockRemover = BlockRemover.of(buildScript, setOf(SimpleBlock("kotlinter"), SimpleBlock("polyrepo")))
    val rewrittenContent = blockRemover.rewritten()
    val expectedContent =
      """
        |plugins {
        |  application
        |}
        |
      """.trimMargin()

    // Then...
    assertEquals(expectedContent, rewrittenContent)
  }

  @Test
  fun `can remove nested SimpleBlock`() {
    val buildScript =
      """
        |plugins {
        |  application
        |}
        |
        |java {
        |  toolchain {
        |    languageVersion = javaTarget
        |  }
        |  sourceCompatibility = javaTarget
        |}
        |
        |polyrepo {
        |  publishToArtifactory = true // cash.server.protos does not publish by default
        |  shortName = "foo"
        |}
        |
      """.trimMargin()

    // When...
    val blockRemover = BlockRemover.of(buildScript, setOf(SimpleBlock("toolchain"), SimpleBlock("polyrepo")))
    val rewrittenContent = blockRemover.rewritten()
    val expectedContent =
      """
        |plugins {
        |  application
        |}
        |
        |java {
        |  sourceCompatibility = javaTarget
        |}
        |
      """.trimMargin()

    // Then...
    assertEquals(expectedContent, rewrittenContent)
  }

  @Test
  fun `can remove TaskWithTypeBlock`() {
    val buildScript =
      """
        |plugins {
        |  application
        |}
        |
        |tasks.withType<org.jmailen.gradle.kotlinter.tasks.LintTask> {
        |  dependsOn("generateMainProtos")
        |  exclude { it.file.path.contains("/build/") }
        |}
        |
        |tasks.withType<org.jmailen.gradle.kotlinter.tasks.FormatTask> {
        |  exclude { it.file.path.contains("/build/") }
        |}
        |
      """.trimMargin()

    // When...
    val blockRemover = BlockRemover.of(buildScript, setOf(
      TaskWithTypeBlock("org.jmailen.gradle.kotlinter.tasks.LintTask"),
      TaskWithTypeBlock("org.jmailen.gradle.kotlinter.tasks.FormatTask"),
      TaskWithTypeBlock("org.jmailen.gradle.kotlinter.tasks.LintTask::class")
    ))

    val rewrittenContent = blockRemover.rewritten()
    val expectedContent =
      """
        |plugins {
        |  application
        |}
        |
      """.trimMargin()

    // Then...
    assertEquals(expectedContent, rewrittenContent)
  }

  @Test
  fun `can remove nested TaskWithTypeBlock`() {
    val buildScript =
      """
        |plugins {
        |  application
        |}
        |
        |allprojects {
        |  tasks.withType(org.jmailen.gradle.kotlinter.tasks.LintTask::class).configureEach {
        |    exclude { it.file.path.contains("/build/") }
        |  }
        |  dependencies {
        |    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.5.31")
        |  }
        |}
        |
      """.trimMargin()

    // When...
    val blockRemover = BlockRemover.of(buildScript, setOf(
      TaskWithTypeBlock("org.jmailen.gradle.kotlinter.tasks.LintTask::class")
    ))

    val rewrittenContent = blockRemover.rewritten()
    val expectedContent =
      """
        |plugins {
        |  application
        |}
        |
        |allprojects {
        |  dependencies {
        |    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.5.31")
        |  }
        |}
        |
      """.trimMargin()

    // Then...
    assertEquals(expectedContent, rewrittenContent)
  }
}