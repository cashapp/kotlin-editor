package cash.recipes.lint.buildscripts.config

import cash.recipes.lint.buildscripts.model.Statement
import java.nio.file.Path
import java.util.function.BiPredicate
import kotlin.io.path.exists

/**
 * Any statement not explicitly allow-listed as treated as a reportable violation. Every permissible statement must
 * therefore be listed.
 */
public class AllowList private constructor(private val specs: List<Spec>) {

  public interface Spec : BiPredicate<Path, Statement> {
    public fun test(path: Path): Boolean
  }

  /** Returns `true` if [buildScript] is an ignored path according to any of the [specs]. */
  public fun isIgnoredBuildScript(buildScript: Path): Boolean {
    return specs.any { spec -> spec.test(buildScript) }
  }

  /**
   * Given a list of [Statement]s from a [buildScript], returns those that aren't explicitly allow-listed per [specs];
   * that is, returns all the forbidden statements.
   */
  public fun forbiddenStatements(buildScript: Path, statements: List<Statement>): List<Statement> {
    return statements.filterNot { stmt -> specs.any { spec -> spec.test(buildScript, stmt) } }
  }

  /** Returns list of lint configs used to create this [AllowList]. May be empty. */
  public fun getLintConfigs(): List<LintConfig> {
    return specs.asSequence()
      .filterIsInstance<YamlSpec>()
      .map { it.lintConfig }
      .toList()
  }

  public companion object {
    /** Create an [AllowList] from one or more yml config files. See [LintConfig] for an example config file. */
    public fun of(vararg files: Path): AllowList = of(files.toList())

    /** Create an [AllowList] from one or more yml config files. See [LintConfig] for an example config file. */
    public fun of(files: List<Path>): AllowList {
      validateYaml(files)
      return of(files.map { file -> YamlSpec.of(file) })
    }

    private fun validateYaml(files: List<Path>) {
      files.forEach { f ->
        require(f.exists()) { "$f does not exist." }
      }
    }

    public fun of(spec: Spec): AllowList = of(listOf(spec))

    public fun of(specs: Iterable<Spec>): AllowList = AllowList(specs.toList())

    // Primarily exists to support the earliest tests. Those tests could be refactored and this deleted.
    public fun ofNamedBlocks(vararg names: String): AllowList {
      val spec = object : Spec {
        override fun test(t: Path, u: Statement): Boolean = (u as? Statement.NamedBlock)?.name in names
        override fun test(path: Path): Boolean = false
      }

      return of(spec)
    }
  }
}
