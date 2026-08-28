package cash.recipes.lint.internal

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

/**
 * Prefer to use [GradleGuardCheckTask][cash.recipes.lint.GradleGuardCheckTask] or
 * [GradleGuardBaselineTask][cash.recipes.lint.GradleGuardBaselineTask].
 */
@CacheableTask
public abstract class GradleGuardTask @Inject constructor(
  private val execOps: ExecOperations
) : DefaultTask() {

  init {
    group = JavaBasePlugin.VERIFICATION_GROUP
    description = "Runs Gradle Guard"
  }

  @get:Classpath
  public abstract val gradleGuardProgram: ConfigurableFileCollection

  @get:Input
  public abstract val projectPath: Property<String>

  @get:Option(option = "root", "Root path (for more human-readable output)")
  @get:Optional
  @get:Input
  public abstract val rootPath: Property<String>

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val buildScripts: ConfigurableFileCollection

  @get:Input
  public abstract val excludes: SetProperty<String>

  @get:Option(
    option = "ignore-exclude",
    "Primarily intended for internal use. If true, ignores the value of 'exclude'."
  )
  @get:Optional
  @get:Input
  public abstract val ignoreExclude: Property<Boolean>

  // check or baseline
  @get:Option(option = "mode", "Mode ('check' or 'baseline')")
  @get:Optional
  @get:Input
  public abstract val mode: Property<String>

  // human or machine
  @get:Option(option = "format", "Console format ('human' or 'machine')")
  @get:Optional
  @get:Input
  public abstract val format: Property<String>

  @get:Option(
    option = "ignore-failure",
    "If true, ignores gradle-guard failures. Useful when running individually on many projects at once."
  )
  @get:Optional
  @get:Input
  public abstract val ignoreFailure: Property<Boolean>

  @get:Option(option = "config", "Config file")
  @get:Optional
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFile
  public abstract val configFile: RegularFileProperty

  @get:Option(option = "output", "Output file")
  @get:Optional
  @get:OutputFile
  public abstract val outputFile: RegularFileProperty

  @get:Option(option = "baseline", "Baseline file")
  @get:Optional
  @get:OutputFile
  public abstract val baselineFile: RegularFileProperty

  @get:Option(option = "verbose", "If false, disables Gradle logging")
  @get:Optional
  @get:Input
  public abstract val verbose: Property<Boolean>

  @TaskAction
  public fun action() {
    val projectPath = projectPath.get()
    val verbose = verbose.getOrElse(true)

    val mode = mode.getOrElse("check")
    require(mode in setOf("check", "baseline")) {
      "Expected 'mode' to be 'check' or 'baseline'. Was '$mode'."
    }

    val format = computeFormat(mode)
    require(format in setOf(null, "human", "machine")) {
      "Expected 'format' to be 'human' or 'machine'. Was '$format'."
    }

    val rootPath = rootPath.orNull
    val config = configFile.optionalFile()
    val baseline = baselineFile.optionalFile()
    // The 'check' mode has no --output option. This is for ergonomics.
    val output = if (mode == "baseline") outputFile.optionalFile() else null

    val excludes = if (ignoreExclude.getOrElse(false)) emptySet() else excludes.get()
    val paths = buildScripts.files.asSequence()
      .filter { f -> excludes.isEmpty() || excludes.any { it !in f.invariantSeparatorsPath } }
      .map { f -> f.absolutePath }
      .toSortedSet()

    require(paths.isNotEmpty()) {
      "Expected at least one build script! 'buildScripts' was empty."
    }

    val ignoreFailure = ignoreFailure.getOrElse(false)
    val ignoreFailureText = if (ignoreFailure) " (ignoring failures) " else ""

    val options = buildList {
      add(mode)

      rootPath?.let { option("--root", it) }
      config?.let { option("--config", it.path) }
      baseline?.let { option("--baseline", it.path) }
      output?.let { option("--output", it.path) }
      format?.let { option("--format", it) }
    }

    val cliArgs = options + buildList {
      // Each path has to be added separately
      paths.forEach { add(it) }
    }

    if (verbose) {
      val shortPaths = paths.take(1)
      val remainder = paths.size - shortPaths.size
      val postfix = if (remainder > 0) {
        " (plus $remainder more) …"
      } else {
        ""
      }
      val pathText = shortPaths.joinToString(separator = " ", postfix = postfix)

      val printableOptions = options.joinToString(separator = " ", postfix = pathText)
      logger.lifecycle("Executing gradle-guard$ignoreFailureText:\n\n    gradle-guard $printableOptions\n")
    }

    val result = execOps.javaexec { spec ->
      with(spec) {
        mainClass.set("cash.recipes.lint.cli.Main")
        classpath = gradleGuardProgram
        args = cliArgs
        isIgnoreExitValue = ignoreFailure
      }
    }

    if (result.exitValue != 0 && verbose) {
      logger.lifecycle("gradle-guard failed for '$projectPath'. See console output for more information.")
    }
  }

  // The 'baseline' mode has no --format option. This is for ergonomics.
  private fun computeFormat(mode: String): String? {
    return if (mode == "check") {
      format.getOrElse("human")
    } else {
      null
    }
  }

  private fun RegularFileProperty.optionalFile(): File? {
    return if (isPresent) get().asFile else null
  }

  private fun MutableList<String>.option(name: String, value: String) {
    add(name)
    add(value)
  }
}