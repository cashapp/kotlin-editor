package cash.grammar.kotlindsl.model

/**
 * Represents a block that can be removed.
 */
public sealed class RemovableBlock {
  /**
   * Represents a simple block with a name.
   *
   * Usage example:
   * ```
   * blockName {
   *     // Block content
   * }
   * ```
   *
   * @property name The name of the simple block.
   */
  public data class SimpleBlock(val name: String) : RemovableBlock()

  /**
   * Represents a task configuration block that supports both Kotlin DSL and Groovy DSL.
   *
   * This block can be used to exclude specific tasks from the build process.
   *
   * Usage example for Kotlin DSL:
   * ```
   * tasks.withType<org.jmailen.gradle.kotlinter.tasks.LintTask> {
   *     exclude { it.file.path.contains("/build/") }
   * }
   * ```
   *
   * Usage example for Groovy DSL:
   * ```
   * tasks.withType(org.jmailen.gradle.kotlinter.tasks.LintTask) {
   *     exclude { it.file.path.contains("/build/") }
   * }
   * ```
   *
   * @property type The type of task to be configured. This should be the value between `<>` in Kotlin DSL and `()` in Groovy DSL.
   */
  public data class TaskWithTypeBlock(val type: String): RemovableBlock()
}

