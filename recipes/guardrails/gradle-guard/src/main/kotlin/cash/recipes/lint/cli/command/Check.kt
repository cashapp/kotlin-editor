package cash.recipes.lint.cli.command

import cash.recipes.lint.buildscripts.reporter.HumanReadableReporter
import cash.recipes.lint.buildscripts.reporter.MachineReadableReporter
import cash.recipes.lint.cli.Logger
import cash.recipes.lint.cli.ProcessResult
import cash.recipes.lint.cli.ProcessResult.Companion.handleResult
import cash.recipes.lint.cli.command.Check.Format
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum

/**
 * Command for checking Gradle Kotlin DSL scripts against allowed content. Example usage:
 * ```
 * gradle-guard check --config=gradle-guard.yml dir-to-lint/
 * ```
 */
internal class Check : BaseGuardCommand(name = "check") {

  enum class Format {
    HUMAN, COMPUTER, MACHINE
  }

  private val format: Format by option()
    .enum<Format>()
    .help("The report output format. Defaults to 'human', which is more verbose and readable. Also accepts 'machine' or 'computer' (aliases for the same thing) which is more concise.")
    .default(Format.HUMAN)

  override fun run() {
    runCatching {
      Checker(
        format = format,
        root = root,
        paths = paths,
        config = config,
        baseline = baseline,
      ).call()
    }.handleResult()
  }
}

// TODO(tsr): consider passing in a `FileSystem` instance and using that to create Paths instead of `Path.of()`
internal class Checker(
  private val format: Format,
  root: String?,
  paths: Set<String>,
  config: String?,
  baseline: String?,
) : LinterAction(
  root = root,
  paths = paths,
  config = config,
  baseline = baseline,
) {

  override fun call(): ProcessResult {
    // TODO(tsr): more advanced logging?
    // Build report
    val logger = Logger()
    val reporter = when (format) {
      Format.HUMAN -> HumanReadableReporter.of(linter, logger)
      Format.COMPUTER, Format.MACHINE -> MachineReadableReporter.of(linter, logger)
    }
    val report = reporter.buildReport()

    if (linter.hasErrors()) {
      logger.error(report)
      return ProcessResult.FAILURE
    } else {
      logger.print(report)
      return ProcessResult.SUCCESS
    }
  }
}
