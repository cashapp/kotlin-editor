package com.squareup.gradle

import com.squareup.gradle.utils.DependencyCatalog
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

internal class BasePlugin(private val project: Project) {

  private val versionCatalog = DependencyCatalog(project).catalog

  fun apply(): Unit = project.run {
    pluginManager.run {
      apply("java-library")
      apply("maven-publish")
      apply("com.autonomousapps.dependency-analysis")
    }

    group = "app.cash.gradle-kotlin"
    // TODO(tsr): not sure if this versioning strategy will work for OSS context.
    version = providers.systemProperty("publish_version").orElse("unversioned").get()

    configureJvmTarget()
    configurePublishing()
  }

  private fun Project.configureJvmTarget() {
    val javaVersion = JavaLanguageVersion.of(
      versionCatalog.findVersion("java").orElseThrow().requiredVersion
    )

    tasks.withType(JavaCompile::class.java).configureEach {
      it.options.release.set(javaVersion.asInt())
    }

    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
      tasks.withType(KotlinJvmCompile::class.java).configureEach {
        it.kotlinOptions {
          jvmTarget = javaVersion.toString()
        }
      }
    }
  }

  // TODO(tsr): currently publishing to internal Artifactory. Must migrate code when we migrate repo
  //  to squareup OSS space.
  private fun Project.configurePublishing() {
    extensions.getByType(JavaPluginExtension::class.java).run {
      withSourcesJar()
    }

    val username = providers.systemProperty("publish_username").orElse("")
    val password = providers.systemProperty("publish_password").orElse("")

    extensions.getByType(PublishingExtension::class.java).run {
      publications { pubs ->
        pubs.create("library", MavenPublication::class.java) { pub ->
          pub.from(components.getAt("java"))
        }
      }

      repositories { repos ->
        repos.maven { repo ->
          repo.name = "artifactory"
          repo.url = uri("TODO(tsr)")
          repo.credentials { credentials ->
            credentials.username = username.get()
            credentials.password = password.get()
          }
        }
      }
    }
  }
}
