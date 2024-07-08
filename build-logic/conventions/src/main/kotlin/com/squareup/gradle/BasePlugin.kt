package com.squareup.gradle

import com.squareup.gradle.utils.DependencyCatalog
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

internal class BasePlugin(private val project: Project) {

  private val versionCatalog = DependencyCatalog(project).catalog

  fun apply(): Unit = project.run {
    pluginManager.run {
      apply("java-library")
      apply("com.vanniktech.maven.publish")
      apply("com.autonomousapps.dependency-analysis")
    }

    // See gradle.properties
    group = providers.gradleProperty("GROUP").get()
    version = providers.gradleProperty("VERSION").get()

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

  private fun Project.configurePublishing() {
    extensions.getByType(MavenPublishBaseExtension::class.java).run {
      publishToMavenCentral(SonatypeHost.DEFAULT, automaticRelease = true)
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

    tasks.named { it == "publishToMavenCentral" }.configureEach { t ->
      t.notCompatibleWithConfigurationCache("Publishing is incompatible")
    }
  }
}
