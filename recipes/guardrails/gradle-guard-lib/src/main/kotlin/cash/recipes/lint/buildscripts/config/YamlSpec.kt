package cash.recipes.lint.buildscripts.config

import cash.recipes.lint.buildscripts.model.Statement
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

public class YamlSpec(public val lintConfig: LintConfig) : AllowList.Spec {

  private val ignoredPaths = lintConfig.getIgnoredPaths().map { Path.of(it) }

  override fun test(path: Path, stmt: Statement): Boolean {
    if (ignoredPaths.any { path.startsWith(it) }) {
      return true
    }

    return when (stmt) {
      is Statement.NamedBlock -> stmt.name in lintConfig.getAllowedBlocks()
      else -> lintConfig.getAllowedPrefixes().any { prefix -> stmt.text.startsWith(prefix) }
    }
  }

  override fun test(path: Path): Boolean {
    return ignoredPaths.any { path.startsWith(it) }
  }

  internal companion object {
    fun of(configFile: Path): YamlSpec {
      val config = LintConfig.parse(Files.newInputStream(configFile, StandardOpenOption.READ))
      return YamlSpec(config)
    }
  }
}
