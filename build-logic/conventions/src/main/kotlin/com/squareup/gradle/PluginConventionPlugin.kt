package com.squareup.gradle

import com.squareup.gradle.utils.DependencyCatalog
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * ```
 * plugins {
 *   id("cash.plugin")
 * }
 * ```
 */
public abstract class PluginConventionPlugin : Plugin<Project> {
  override fun apply(target: Project): Unit = target.run {
    pluginManager.run {
      apply("java-gradle-plugin")
      apply("com.gradle.plugin-publish")
      apply(LibraryConventionPlugin::class.java)
    }

    configureTestKitSupport()
  }

  private fun Project.configureTestKitSupport() {
    // TODO(tsr): I hate this and want to figure out why this is happening
    tasks.named { it == "publishTestKitSupportForJavaPublicationToFunctionalTestRepository" }.configureEach { t ->
      t.dependsOn("signPluginMavenPublication")
    }
    tasks.named { it == "publishPluginMavenPublicationToFunctionalTestRepository" }.configureEach { t ->
      t.dependsOn("signTestKitSupportForJavaPublication")
    }
    tasks.named { it == "publishPluginMavenPublicationToMavenCentralRepository" }.configureEach { t ->
      t.dependsOn("signTestKitSupportForJavaPublication")
    }

    val versionCatalog = DependencyCatalog(this).catalog
    dependencies.run {
      //  functionalTestImplementation(platform(libs.junit5.bom))
      add("functionalTestImplementation", versionCatalog.findLibrary("assertj").orElseThrow())
      add("functionalTestImplementation", versionCatalog.findLibrary("junit.jupiter.api").orElseThrow())
      add("functionalTestImplementation", versionCatalog.findLibrary("testkit.support").orElseThrow())
      add("functionalTestRuntimeOnly", versionCatalog.findLibrary("junit.jupiter.engine").orElseThrow())
      add("functionalTestRuntimeOnly", versionCatalog.findLibrary("junit.platform.launcher").orElseThrow())
    }
  }
}
