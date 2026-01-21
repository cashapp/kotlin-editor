package cash.recipes.lint.cli.command

import cash.recipes.lint.buildscripts.reporter.Logger
import cash.recipes.lint.cli.ConsoleLogger
import cash.recipes.lint.cli.ProcessResult
import cash.recipes.lint.cli.ProcessResult.Companion.handleResult
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import java.nio.file.Path

/**
 * Command for generating a baseline. Example usage:
 * ```
 * gradle-guard baseline [--config=gradle-guard.yml] --output=gradle-guard-baseline.yml dir-to-lint/
 * ```
 */
internal class Baseline : BaseGuardCommand(name = "baseline") {

  private val output: String? by option().help("The path to the new baseline file. If not present, then `baseline` is used.")

  override fun run() {
    runCatching {
      Baseliner(
        logger = ConsoleLogger(this),
        root = root,
        paths = paths,
        config = config,
        baseline = baseline,
        output = output,
      ).call()
    }.handleResult()
  }
}

internal class Baseliner(
  logger: Logger,
  root: String?,
  paths: Set<String>,
  config: String?,
  private val baseline: String?,
  private val output: String?,
) : LinterAction(
  logger = logger,
  root = root,
  paths = paths,
  config = config,
  baseline = baseline,
) {

  private companion object {
    const val OUTPUT_NOT_AVAILABLE_ERROR =
      "Both `baseline` and `output` are null. Expected one non-null for generating a baseline file."
  }

  init {
    require(baseline != null || output != null) { OUTPUT_NOT_AVAILABLE_ERROR }
  }

  override fun call(): ProcessResult {
    linter.writeBaseline(dest())
    return ProcessResult.SUCCESS
  }

  private fun dest(): Path {
    return if (output != null) {
      Path.of(output)
    } else if (baseline != null) {
      Path.of(baseline)
    } else {
      // Should be impossible thanks to init block, but this makes the compiler happy without recourse to `!!`.
      error(OUTPUT_NOT_AVAILABLE_ERROR)
    }
  }
}
