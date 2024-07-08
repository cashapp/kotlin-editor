package com.squareup.gradle

import com.squareup.gradle.utils.DependencyCatalog
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.plugins.antlr.AntlrTask
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar

@Suppress("unused")
public class GrammarConventionPlugin : Plugin<Project> {

  private lateinit var versionCatalog: VersionCatalog

  override fun apply(target: Project): Unit = target.run {
    pluginManager.run {
      apply("antlr")
    }
    BasePlugin(this).apply()

    versionCatalog = DependencyCatalog(this).catalog

    configureAntlr()
  }

  private fun Project.configureAntlr() {
    val taskGroup = "grammar"

    val pkg = "com.squareup.cash.grammar"
    val dir = pkg.replace('.', '/')
    val antlrOutput = layout.buildDirectory.dir("generated-src/antlr/main/$dir")
    val antlrSrc = "src/main/antlr/$dir"

    val copyAntlrTokens = tasks.register("copyAntlrTokens", Copy::class.java) { t ->
      t.group = taskGroup
      t.description =
        "Copies grammar tokens from build dir into main source for better IDE experience"

      t.from(antlrOutput) {
        it.include("*.tokens")
      }
      t.into(antlrSrc)
    }

    val generateGrammarSource = tasks.named("generateGrammarSource", AntlrTask::class.java) { t ->
      t.group = taskGroup
      t.description = "Generates Java listener and visitor source from .g4 files"

      // the IDE complains if the .tokens files aren't in the main source dir. This isn't necessary for
      // builds to succeed, but it is necessary for a good IDE experience.
      t.finalizedBy(copyAntlrTokens)

      t.outputDirectory = file(layout.buildDirectory.dir("generated-src/antlr/main/$dir"))
      t.arguments = t.arguments + listOf(
        // Specify the package declaration for generated Java source
        "-package", pkg,
        // Specify that generated Java source should go into the outputDirectory, regardless of package structure
        "-Xexact-output-dir",
        // Specify the location of "libs"; i.e., for grammars composed of multiple files
        "-lib", antlrSrc,
        // We want visitors alongside listeners
        "-visitor",
      )
    }

    val generateTestGrammarSource = tasks.named("generateTestGrammarSource")

    // TODO(tsr): There is probably a better way to do this.
    // Even though we're excluding the token files from the source jar, we still need to specify the
    // task dependency to satisfy Gradle.
    // nb: the sourcesJar task must be getting added in an afterEvaluate from the mavenPublish
    // plugin, so I have to use this lazy approach to configure it.
    tasks.withType(Jar::class.java).named { it == "sourcesJar" }.configureEach { t ->
      t.dependsOn(copyAntlrTokens)
      t.exclude("**/*.tokens")
    }

    // Workaround for https://github.com/gradle/gradle/issues/19555#issuecomment-1593252653
    extensions.getByType(SourceSetContainer::class.java).run {
      getAt("main").java.srcDir(generateGrammarSource.map { files() })
      getAt("test").java.srcDir(generateTestGrammarSource.map { files() })
    }

    // Excluding icu4j because it bloats artifact size significantly
    configurations
      .getByName("runtimeClasspath")
      .exclude(mapOf("group" to "com.ibm.icu", "module" to "icu4j"))

    dependencies.run {
      add("antlr", versionCatalog.findLibrary("antlr.core").orElseThrow())
      add("runtimeOnly", versionCatalog.findLibrary("antlr.runtime").orElseThrow())
    }
  }
}
