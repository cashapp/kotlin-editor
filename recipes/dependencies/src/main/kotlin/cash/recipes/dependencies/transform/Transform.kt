package cash.recipes.dependencies.transform

public data class Transform(
  public val from: Element,
  public val to: Element,
) {

  public fun from(): String = from.render()
  public fun to(): String = to.render()

  public sealed class Element {

    public abstract fun render(): String

    /** A "raw string" declaration, like `com.foo:bar:1.0`, with or without the version string. */
    public data class StringLiteral(public val value: String) : Element() {
      // wrap in quotation marks (because it's a string literal!)
      override fun render(): String = "\"$value\""
    }

    /** A dependency accessor, like `libs.fooBar`. Doesn't need to represent a version catalog entry. */
    public data class Accessor(public val value: String) : Element() {
      override fun render(): String = value
    }

    public fun matches(other: String): Boolean = render() == other
  }
}
