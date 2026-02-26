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

  public companion object {
    // When merging two LintConfigs, we need to do a complex merge on the BaselineConfig of each so we don't get
    // multiple entries for the same project.
    public fun merge(left: Set<BaselineConfig>, right: Set<BaselineConfig>): Set<BaselineConfig> {
      val map = mutableMapOf<String, BaselineConfig>()
      map.putAll(left.associateBy { it.path })
      right.forEach { config ->
        map.merge(config.path, config) { acc, inc ->
          BaselineConfig(
            path = acc.path,
            allowedBlocks = (acc.getAllowedBlocks() + inc.getAllowedBlocks()).toSortedSet(),
            allowedPrefixes = (acc.getAllowedPrefixes() + inc.getAllowedPrefixes()).toSortedSet(),
          )
        }
      }

      return map.values.toSortedSet()
    }
  }
}
