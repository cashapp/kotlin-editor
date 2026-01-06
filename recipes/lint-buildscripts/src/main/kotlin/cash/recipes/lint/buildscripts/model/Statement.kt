package cash.recipes.lint.buildscripts.model

/**
 * TODO: docs.
 */
public sealed class Statement(
  public val text: String,
  public open val start: Position,
  public open val stop: Position,
) {

  // TODO(tsr): handle labels and annotations?

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

/**
 * TODO: docs.
 */
public data class Position(
  public val line: Int,
  public val positionInLine: Int,
)
