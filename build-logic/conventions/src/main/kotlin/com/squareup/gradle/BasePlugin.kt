package com.squareup.gradle

import com.squareup.gradle.extension.KotlinEditorExtension
import com.squareup.gradle.utils.DependencyCatalog
import com.vanniktech.maven.publish.DeploymentValidation
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

internal class BasePlugin(private val project: Project) {

  private val versionCatalog = DependencyCatalog(project).catalog

  fun apply(): Unit = project.run {
    pluginManager.run {
      apply("java-library")
      apply("com.vanniktech.maven.publish")
      apply("com.autonomousapps.dependency-analysis")
    }

    KotlinEditorExtension.of(project)

    // See gradle.properties
    // These values may be overridden in individual projects by configuring the `kotlinEditor` extension.
    group = providers.gradleProperty("GROUP").get()
    // When publishing gradle-guard, we need non-snapshot versions of its dependencies. So, the GHA workflow named
    // `publish-gradle-guard` has a required input `core_libs_version` that will override the current (probably
    // "development" or snapshot) of those core libs.
    version = providers.gradleProperty("cashapp.core-libs-version")
      .orElse(providers.gradleProperty("VERSION"))
      .get()

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
      tasks.withType(KotlinJvmCompile::class.java).configureEach { t ->
        t.compilerOptions {
          jvmTarget.set(JvmTarget.fromTarget(javaVersion.toString()))
        }
      }
    }
  }

  private fun Project.configurePublishing() {
    // Leads to task named `publishMavenPublicationToLocalRepository` that publishes to `<project-dir>/build/repo/`.
    extensions.getByType(PublishingExtension::class.java).let { publishing ->
      publishing.repositories { repos ->
        repos.maven { repo ->
          repo.name = "local"
          repo.url = uri(layout.buildDirectory.dir("repo"))
        }
      }
    }

    extensions.getByType(MavenPublishBaseExtension::class.java).run {
      publishToMavenCentral(automaticRelease = true, DeploymentValidation.VALIDATED)
      signAllPublications()

      pom { p ->
        p.name.set(project.name)
        p.description.set("A library for parsing, rewriting, and linting Kotlin source code")
        p.inceptionYear.set("2024")
        p.url.set("https://github.com/cashapp/kotlin-editor")

        p.licenses { licenses ->
          licenses.license { l ->
            l.name.set("The Apache Software License, Version 2.0")
            l.url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            l.distribution.set("repo")
          }
        }
        p.developers { devs ->
          devs.developer { d ->
            d.id.set("cashapp")
            d.name.set("Cash App")
            d.url.set("https://github.com/cashapp")
          }
        }
        p.scm { scm ->
          scm.url.set("https://github.com/cashapp/kotlin-editor")
          scm.connection.set("scm:git:git://github.com/cashapp/kotlin-editor.git")
          scm.developerConnection.set("scm:git:ssh://git@github.com/cashapp/kotlin-editor.git")
        }
      }
    }
  }
}
