package cash.grammar.kotlindsl.model

import com.squareup.cash.grammar.KotlinParser.AssignmentContext
import org.antlr.v4.runtime.ParserRuleContext

/**
 * Represents an assignment statement within a Gradle build script.
 * An assignment statement is any statement that includes an "=" operator to set a value.
 *
 * ## Examples:
 * - Top-level assignment:
 * ```
 * projectName = "project-a"
 * ```
 *
 * - Assignment within [cash.grammar.kotlindsl.utils.Blocks]:
 * ```
 * foo {
 *   projectName = "project-a"
 * }
 * ```
 *
 * @property id the identifier on the left side of the "=" operator.
 * @property value the value being assigned to the identifier.
 */
public data class Assignment(
  public val id: String,
  public val value: String
) {

  /**
   * For example, `publishToArtifactory = true`
   */
  public fun asString(): String = buildString {
    append(id)
    append(" = ")
    append(value)
  }

  public companion object {
    /**
     * Extracts an [Assignment] from the specified [line] if it represents an assignment within a block.
     *
     * ## Example:
     * ```
     * foo {
     *   projectName = "project-a"
     * }
     * ```
     */
    public fun extractFromBlock(line: ParserRuleContext): Assignment? {
      when (line) {
        is AssignmentContext -> {
          val identifierId = line.directlyAssignableExpression().simpleIdentifier().Identifier().text
          val identifierValue = line.expression().text

          return Assignment(identifierId, identifierValue)
        }
        else -> return null
      }
    }
  }
}
