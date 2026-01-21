package cash.recipes.lint.buildscripts.config

/** @see [LintConfig] */
public data class BaselineConfig(
  private val path: String,
  private val allowedBlocks: Set<String>? = null,
  private val allowedPrefixes: Set<String>? = null,
) {

  internal companion object {
    val PATH_COMPARATOR = Comparator<BaselineConfig> { left, right ->
      left.path.compareTo(right.path)
    }
  }

  public fun getPath(): String = path

  public fun getAllowedBlocks(): Set<String> = allowedBlocks.orEmpty()

  public fun getAllowedPrefixes(): Set<String> = allowedPrefixes.orEmpty()
}
