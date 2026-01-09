package cash.recipes.lint.buildscripts

import cash.recipes.lint.buildscripts.config.LintConfig
import cash.recipes.lint.buildscripts.model.Statement
import java.nio.file.Path
import java.util.function.BiPredicate

public class AllowList private constructor(private val specs: List<Spec>) {

  public interface Spec : BiPredicate<Path, Statement> {
    public fun test(path: Path): Boolean
  }

  // TODO move this class to its own file.
  internal class YamlSpec(private val lintConfig: LintConfig) : Spec {

    private val ignoredPaths = lintConfig.ignoredPaths().map { Path.of(it) }

    override fun test(path: Path, stmt: Statement): Boolean {
      if (ignoredPaths.any { path.startsWith(it) }) {
        return true
      }

      return when (stmt) {
        is Statement.NamedBlock -> stmt.name in lintConfig.allowedBlocks()
        else -> lintConfig.allowedPrefixes().any { prefix -> stmt.text.startsWith(prefix) }
      }
    }

    override fun test(path: Path): Boolean {
      return ignoredPaths.any { path.startsWith(it) }
    }
  }

  public fun isIgnoredBuildScript(buildScript: Path): Boolean {
    return specs.any { spec -> spec.test(buildScript) }
  }

  public fun forbiddenStatements(buildScript: Path, statements: List<Statement>): List<Statement> {
    return statements.filterNot { stmt -> specs.any { spec -> spec.test(buildScript, stmt) } }
  }

  public companion object {
    public fun of(yaml: String): AllowList {
      val config = LintConfig.parse(yaml)
      return of(YamlSpec(config))
    }

    public fun of(spec: Spec): AllowList {
      return of(listOf(spec))
    }

    public fun of(vararg names: String): AllowList {
      val spec = object : Spec {
        override fun test(t: Path, u: Statement): Boolean = u.text in names
        override fun test(path: Path): Boolean = false
      }

      return of(spec)
    }

    public fun of(specs: Iterable<Spec>): AllowList {
      return AllowList(specs.toList())
    }
  }
}
