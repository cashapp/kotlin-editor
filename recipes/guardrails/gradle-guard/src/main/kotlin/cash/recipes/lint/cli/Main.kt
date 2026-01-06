package cash.recipes.lint.cli

import cash.recipes.lint.cli.command.Baseline
import cash.recipes.lint.cli.command.Check
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands

/**
 * Run it:
 * ```
 * # Dev mode
 * ./gradlew recipes:guardrails:gradle-guard:run --args="..."
 * ```
 * ```
 * # As a final binary
 * # 1. Install dist
 * ./gradlew recipes:guardrails:gradle-guard:installDist
 *
 * # 2. Run binary
 * ./recipes/guardrails/gradle-guard/build/install/gradle-guard/bin/gradle-guard check --config=... --baseline=... <path>
 * ```
 *
 * @see [Check]
 * @see [Baseline]
 * @see <a href="http://ajalt.github.io/clikt/">Clikt documentation</a>
 */
public class Main : NoOpCliktCommand(name = "gradle-guard") {

  override val printHelpOnEmptyArgs: Boolean = true

  public companion object {
    public const val SUCCESS: Int = 0
    public const val FAILURE: Int = 1

    @JvmStatic
    public fun main(vararg args: String) {
      Main()
        .subcommands(
          Check(),
          Baseline(),
        )
        .main(args)
    }
  }
}
