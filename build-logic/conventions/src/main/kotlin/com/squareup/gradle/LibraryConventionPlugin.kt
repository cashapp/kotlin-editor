package com.squareup.gradle

import com.autonomousapps.DependencyAnalysisSubExtension
import com.squareup.gradle.utils.DependencyCatalog
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

@Suppress("unused")
public class LibraryConventionPlugin : Plugin<Project> {

  private lateinit var versionCatalog: VersionCatalog

  override fun apply(target: Project): Unit = target.run {
    pluginManager.run {
      apply("org.jetbrains.kotlin.jvm")
    }
    BasePlugin(this).apply()

    versionCatalog = DependencyCatalog(this).catalog

    configureTests()
    configureKotlin()
  }

  private fun Project.configureKotlin() {
    extensions.getByType(KotlinJvmProjectExtension::class.java).run {
      explicitApi()
    }
  }

  private fun Project.configureTests() {
    tasks.withType(Test::class.java).configureEach {
      it.useJUnitPlatform()
    }

    dependencies.run {
      add("testImplementation", versionCatalog.findLibrary("assertj").orElseThrow())
      add("testImplementation", versionCatalog.findLibrary("junit.jupiter.api").orElseThrow())
      add("testRuntimeOnly", versionCatalog.findLibrary("junit.jupiter.engine").orElseThrow())
    }

    // These default dependencies are added to all library modules. Don't warn/fail if they're not
    // used.
    extensions.getByType(DependencyAnalysisSubExtension::class.java).run {
      issues { issueHandler ->
        issueHandler.onUnusedDependencies { issue ->
          issue.exclude(
            versionCatalog.findLibrary("assertj").orElseThrow(),
            versionCatalog.findLibrary("junit.jupiter.api").orElseThrow(),
          )
        }
      }
    }
  }
}
