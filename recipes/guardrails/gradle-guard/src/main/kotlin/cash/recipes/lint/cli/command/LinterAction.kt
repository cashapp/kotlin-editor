package cash.recipes.lint.cli.command

import cash.recipes.lint.buildscripts.Linter
import cash.recipes.lint.buildscripts.config.AllowList
import cash.recipes.lint.buildscripts.reporter.Logger
import cash.recipes.lint.cli.ProcessResult
import java.nio.file.Path
import java.util.concurrent.Callable

internal abstract class LinterAction(
  protected val logger: Logger,
  private val root: String?,
  private val paths: Set<String>,
  private val config: String?,
  private val baseline: String?,
) : Callable<ProcessResult> {

  protected val linter: Linter by lazy(LazyThreadSafetyMode.NONE) {
    // Build allow list
    val configFiles = listOfNotNull(baseline, config).map { s -> Path.of(s) }
    val allowList = AllowList.of(configFiles)

    // Build linter
    val paths = paths.map { Path.of(it) }
    val root = root?.let { Path.of(it) }
    Linter.of(allowList, paths, root)
  }
}
