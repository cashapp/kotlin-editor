package cash.recipes.lint.buildscripts

import cash.recipes.lint.buildscripts.model.Statement
import java.nio.file.Path
import java.util.function.BiPredicate

public class AllowList private constructor(private val specs: List<Spec>) {

  public interface Spec : BiPredicate<Path, Statement>

  public fun forbiddenStatements(buildScript: Path, statements: List<Statement>): List<Statement> {
    return statements.filterNot { stmt -> specs.any { spec -> spec.test(buildScript, stmt) } }
  }

  public companion object {
    public fun of(vararg names: String): AllowList {
      val spec = object : Spec {
        override fun test(t: Path, u: Statement): Boolean = u.text in names
      }

      return AllowList(listOf(spec))
    }
  }
}
