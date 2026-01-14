package cash.recipes.lint.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.unique
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option

internal abstract class BaseGuardCommand(name: String) : CliktCommand(name = name) {

  override val printHelpOnEmptyArgs: Boolean = true

  protected val root: String? by option().help("The root directory. Useful for human readability and necessary for baseline functionality when `paths` includes multiple build scripts.")

  protected val config: String? by option().help("The path to the config file.")

  protected val baseline: String? by option().help("The path to the baseline file.")

  protected val paths: Set<String> by argument(
    help = "May be either a single 'gradle.kts' file, a directory, or a combination of the two. All directories will be walked recursively to find every 'gradle.kts' file."
  ).multiple(required = true).unique()
}
