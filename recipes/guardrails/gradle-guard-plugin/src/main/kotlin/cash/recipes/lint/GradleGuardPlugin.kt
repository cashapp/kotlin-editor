package cash.recipes.lint

import cash.recipes.lint.GradleGuardPlugin.KotlinDslValueSource.Parameters
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Bundling
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import java.io.File

@Suppress("UnstableApiUsage")
public abstract class GradleGuardPlugin : Plugin<Project> {
  override fun apply(target: Project): Unit = target.run {
    val extension = GradleGuardExtension.create(this)

    val scope = configurations.dependencyScope("gradleGuard") { c ->
      c.description = "The declared version of gradle-guard."
      c.defaultDependencies { deps ->
        deps.addLater(extension.version.map { v ->
          dependencies.create("app.cash.gradle-guard:gradle-guard:$v")
        })
      }
    }
    val resolvable = configurations.resolvable("gradleGuardClasspath") { c ->
      c.description = "The classpath that resolves the graph for gradle-guard."
      c.extendsFrom(scope)

      // gradle-guard uses the Shadow plugin.
      c.attributes { a ->
        a.attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling::class.java, Bundling.SHADOWED))
      }
    }

    val projectPath = path

    tasks.register("gradleGuardCheck", GradleGuardCheckTask::class.java) { t ->
      t.projectPath.set(projectPath)
      t.gradleGuardProgram.setFrom(resolvable)
      t.mode.set("check")

      t.baselineFile.set(extension.baselineFile)
      t.configFile.set(extension.configFile)
      // check mode has no --output option

      t.format.set(extension.format)
      t.excludes.set(extension.excludes)
      t.rootPath.set(isolated.rootProject.projectDirectory.asFile.path)
      t.buildScripts.setFrom(KotlinDslValueSource.of(this))
    }

    tasks.register("gradleGuardBaseline", GradleGuardBaselineTask::class.java) { t ->
      t.projectPath.set(projectPath)
      t.gradleGuardProgram.setFrom(resolvable)
      t.mode.set("baseline")

      t.baselineFile.set(extension.baselineFile)
      t.configFile.set(extension.configFile)
      t.outputFile.set(extension.outputFile)

      // baseline mode has no --format option
      t.excludes.set(extension.excludes)
      t.rootPath.set(isolated.rootProject.projectDirectory.asFile.path)
      t.buildScripts.setFrom(KotlinDslValueSource.of(this))
    }
  }

  /** We wrap the build script search in a value source to hide it from Gradle's configuration cache input detector. */
  internal abstract class KotlinDslValueSource : ValueSource<Set<File>, Parameters> {
    override fun obtain(): Set<File> {
      return parameters.rootDir.asFileTree
        .filter { f -> f.name.endsWith(".gradle.kts") }
        .filter { f -> f.exists() && f.length() > 0 }
        .files
    }

    interface Parameters : ValueSourceParameters {
      val rootDir: DirectoryProperty
    }

    internal companion object {
      fun of(project: Project): Provider<Set<File>> {
        return project.providers.of(KotlinDslValueSource::class.java) { spec ->
          spec.parameters.rootDir.set(project.rootDir)
        }
      }
    }
  }
}
