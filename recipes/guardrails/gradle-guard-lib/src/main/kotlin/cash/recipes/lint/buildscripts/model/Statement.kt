package cash.recipes.lint.buildscripts.model

import java.nio.file.Path

/**
 * A collection of [reports], each for a single [Report.buildScript]. [root] is at the root of the directory hierarchy
 * that contains each of these build scripts. May be null.
 */
public data class ReportCollection(public val reports: List<Report>) {
  public fun hasErrors(): Boolean = reports.any { it.statements.isNotEmpty() }
}

/** A report of [statements] for a single [buildScript]. */
public data class Report(
  public val buildScript: Path,
  public val statements: List<Statement>,
)

/**
 * See `KotlinParser.g4` in the `:grammar` project for more information. Essentially, a Gradle Kotlin DSL script may
 * contain any of the following top-level statements (in alphabetical order):
 *
 * 1. [Assignments][Assignment].
 * 2. [Declarations][Declaration].
 * 3. [Expressions][Expression].
 * 4. [Loops][Loop].
 * 5. [Named blocks][NamedBlock].
 *
 * The words used above are direct references to terms in the grammar. The most common type is named blocks. For
 * example, `plugins`, `dependencies`, etc. Arguably,* a well-written Gradle build script is declarative, containing
 * only a small number of well-known blocks.
 *
 * Note that there may also be labels and annotations in a build script, but these will always be associated with one
 * of the other statement types, and so don't require a data class here.
 *
 * *This library is essentially a complicated argument to this end.
 */
public sealed class Statement(
  public val text: String,
  public open val start: Position,
  public open val stop: Position,
) {

  public data class NamedBlock(
    public val name: String,
    public override val start: Position,
    public override val stop: Position,
  ) : Statement(name, start, stop)

  public data class Assignment(
    public val firstLine: String,
    public override val start: Position,
    public override val stop: Position,
  ) : Statement(firstLine, start, stop)

  public data class Declaration(
    public val firstLine: String,
    public override val start: Position,
    public override val stop: Position,
  ) : Statement(firstLine, start, stop)

  public data class Expression(
    public val firstLine: String,
    public override val start: Position,
    public override val stop: Position,
  ) : Statement(firstLine, start, stop)

  public data class Loop(
    public val firstLine: String,
    public override val start: Position,
    public override val stop: Position,
  ) : Statement(firstLine, start, stop)
}

public data class Position(
  public val line: Int,
  public val positionInLine: Int,
)
