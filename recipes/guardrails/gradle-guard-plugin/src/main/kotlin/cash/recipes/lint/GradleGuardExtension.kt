package cash.recipes.lint

import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import java.io.BufferedReader
import java.io.File
import javax.inject.Inject

/**
 * ```
 * gradleGuard {
 *   // Used in 'check' and 'baseline' modes.
 *   // None by default (nothing is auto-allow-listed).
 *   configFile()
 *
 *   // Used in 'check' and 'baseline' modes.
 *   // None by default.
 *   baselineFile()
 *
 *   // Used in the 'baseline' mode. The path to the new baseline file.
 *   // If not present, then `baseline` is used.
 *   outputFile()
 *
 *   // By default, gradle-guard excludes build scripts with "/build/" in their paths.
 *   excludes("/build/")
 *
 *   // By default, emit console output in 'machine' (concise) format.
 *   // Use "human" for more verbose and readable output.
 *   format("machine")
 *
 *   // Defaults to latest at time of plugin publication.
 *   version(/* custom version */)
 * }
 * ```
 */
public abstract class GradleGuardExtension @Inject constructor(project: Project) {

  private val layout = project.layout
  private val objects = project.objects

  internal val baselineFile: RegularFileProperty = objects.fileProperty()
  internal val configFile: RegularFileProperty = objects.fileProperty()
  internal val outputFile: RegularFileProperty = objects.fileProperty()

  internal val excludes: SetProperty<String> = objects.setProperty(String::class.java)
    // a gross little heuristic to auto-exclude generated build scripts
    .convention(setOf("/build/"))

  internal val format: Property<String> = objects.property(String::class.java)
    .convention("machine")

  /** Defines a custom version of the SortDependencies CLI to use. */
  internal val version: Property<String> = objects.property(String::class.java)
    .convention(
      javaClass.classLoader.getResourceAsStream(VERSION_FILENAME)
        ?.bufferedReader()
        ?.use(BufferedReader::readLine)
        ?: error("Can't find '$VERSION_FILENAME'")
    )

  public fun baselineFile(baselineFile: File) {
    this.baselineFile.set(baselineFile)
    this.baselineFile.disallowChanges()
  }

  public fun baselineFile(baselineFile: String) {
    this.baselineFile.set(layout.projectDirectory.file(baselineFile))
    this.baselineFile.disallowChanges()
  }

  public fun configFile(configFile: File) {
    this.configFile.set(configFile)
    this.configFile.disallowChanges()
  }

  public fun configFile(configFile: String) {
    this.configFile.set(layout.projectDirectory.file(configFile))
    this.configFile.disallowChanges()
  }

  public fun outputFile(outputFile: File) {
    this.outputFile.set(outputFile)
    this.outputFile.disallowChanges()
  }

  public fun outputFile(outputFile: String) {
    this.outputFile.set(layout.projectDirectory.file(outputFile))
    this.outputFile.disallowChanges()
  }

  public fun excludes(vararg excludes: String) {
    excludes(excludes.toList())
  }

  public fun excludes(excludes: Collection<String>) {
    this.excludes.addAll(excludes)
    this.excludes.disallowChanges()
  }

  public fun format(format: String) {
    this.format.set(format)
    this.format.disallowChanges()
  }

  public fun version(version: String) {
    this.version.set(version)
    this.version.disallowChanges()
  }

  internal companion object {
    private const val VERSION_FILENAME = "app-cash-gradle-guard-version.txt"

    fun create(project: Project): GradleGuardExtension {
      return project.extensions.create("gradleGuard", GradleGuardExtension::class.java, project)
    }
  }
}
