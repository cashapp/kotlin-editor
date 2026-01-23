package cash.recipes.lint.buildscripts.config

import cash.recipes.lint.buildscripts.model.Statement
import java.nio.file.Path

public class YamlSpec(public val lintConfig: LintConfig) : AllowList.Spec {

  private val ignoredPaths = lintConfig.getIgnoredPaths().map { Path.of(it) }

  override fun test(path: Path, stmt: Statement): Boolean {
    if (isIgnoredPath(path)) {
      return true
    }

    return when (stmt) {
      is Statement.NamedBlock -> stmt.name in getAllowedBlocksFor(path)
      else -> getAllowedPrefixesFor(path).any { prefix -> stmt.text.startsWith(prefix) }
    }
  }

  private fun getAllowedBlocksFor(path: Path): Set<String> {
    return lintConfig.getAllowedBlocks() + findBaseline(path)?.getAllowedBlocks().orEmpty()
  }

  private fun getAllowedPrefixesFor(path: Path): Set<String> {
    return lintConfig.getAllowedPrefixes() + findBaseline(path)?.getAllowedPrefixes().orEmpty()
  }

  private fun findBaseline(path: Path): BaselineConfig? {
    return lintConfig.getBaseline().firstOrNull { it.getPath() == path.toString() }
  }

  override fun test(path: Path): Boolean {
    return isIgnoredPath(path)
  }

  private fun isIgnoredPath(path: Path): Boolean {
    return ignoredPaths.any { path.startsWith(it) }
  }

  internal companion object {
    fun of(configFile: Path): YamlSpec = YamlSpec(LintConfig.parse(configFile))
  }
}
