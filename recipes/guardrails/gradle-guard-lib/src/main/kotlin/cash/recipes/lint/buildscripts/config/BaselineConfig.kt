package cash.recipes.lint.buildscripts.config

import cash.recipes.lint.buildscripts.utils.comparable.LexicographicIterableComparator

/** @see [LintConfig] */
public data class BaselineConfig(
  private val path: String,
  private val allowedBlocks: Set<String>? = null,
  private val allowedPrefixes: Set<String>? = null,
) : Comparable<BaselineConfig> {

  override fun compareTo(other: BaselineConfig): Int {
    return compareBy(BaselineConfig::getPath)
      .thenBy(LexicographicIterableComparator()) { it.getAllowedBlocks() }
      .thenBy(LexicographicIterableComparator()) { it.getAllowedPrefixes() }
      .compare(this, other)
  }

  public fun getPath(): String = path

  public fun getAllowedBlocks(): Set<String> = allowedBlocks.orEmpty().toSortedSet()

  public fun getAllowedPrefixes(): Set<String> = allowedPrefixes.orEmpty().toSortedSet()
}
