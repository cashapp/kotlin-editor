package cash.grammar.kotlindsl.utils

import cash.grammar.kotlindsl.model.Plugin
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PluginFinderTest {

  @Test fun `should find all plugins`() {
    val buildScript = """
      buildscript {
        repositories {
          maven(url = "https://foo")
        }
      }

    plugins {
      id("cash.server.artifact-group") version libs.cashServerConventionPlugins.get().version
      application
      alias(libs.plugins.cashServerArtifactGroup)
      alias(libs.plugins.dockerCompose) apply false
      kotlin("plugin.serialization")
    }

    apply(plugin = "kotlin")
    apply(plugin = "com.github.johnrengelman.shadow")
    apply(mapOf("plugin" to "foo"))

    """.trimIndent()

    val pluginFinder = PluginFinder.of(buildScript)

    assertThat(pluginFinder.plugins).isEqualTo(
      setOf(
        Plugin(Plugin.Type.BLOCK_ID, "cash.server.artifact-group", version = "libs.cashServerConventionPlugins.get().version"),
        Plugin(Plugin.Type.BLOCK_SIMPLE, "application"),
        Plugin(Plugin.Type.BLOCK_ALIAS, "libs.plugins.cashServerArtifactGroup"),
        Plugin(Plugin.Type.BLOCK_ALIAS, "libs.plugins.dockerCompose", applied = false),
        Plugin(Plugin.Type.BLOCK_KOTLIN, "plugin.serialization"),
        Plugin(Plugin.Type.APPLY, "kotlin"),
        Plugin(Plugin.Type.APPLY, "com.github.johnrengelman.shadow"),
        Plugin(Plugin.Type.APPLY, "foo"),
      )
    )
  }

  @Test fun `finds plugins, ignoring enclosing blocks`() {
    val buildScript = """
      plugins {
        kotlin("jvm")
        `java-gradle-plugin`
        `maven-publish`
      }
      
      gradlePlugin {
        plugins {
          create("my-plugin") {
            id = "com.test.example"
            displayName = "Example"
            implementationClass = "com.test.example.ExamplePlugin"
          }
        }
      }
    """.trimIndent()

    val pluginFinder = PluginFinder.of(buildScript)

    assertThat(pluginFinder.plugins).isEqualTo(
      setOf(
        Plugin(Plugin.Type.BLOCK_KOTLIN, "jvm"),
        Plugin(Plugin.Type.BLOCK_BACKTICK, "java-gradle-plugin"),
        Plugin(Plugin.Type.BLOCK_BACKTICK, "maven-publish"),
      )
    )
  }

  @Test fun `can extract plugin IDs from allprojects blocks`() {
    // ads-advertiser/build.gradle.kts
    val buildScript = """
      import org.jmailen.gradle.kotlinter.tasks.LintTask

      buildscript {
        repositories {
          maven(url = "https://maven.global.square/artifactory/square-public")
        }

        dependencies {
          classpath(libs.kotlinxKover)
          classpath(platform(libs.kotlinGradleBom))
          classpath(libs.kotlinGradlePlugin)
          classpath(libs.cashDevSourcesPlugin)
          classpath(libs.kotlinter)
          classpath(libs.cashPolyrepoPlugin)
          classpath(libs.protobufGradlePlugin)
          classpath(libs.schemaRegistryPlugin)
          classpath(libs.shadowJarPlugin)
        }
      }

      allprojects {
        apply(plugin = "polyrepo-plugin")
        apply(plugin = "org.jmailen.kotlinter")

        tasks {
          withType<LintTask>().configureEach {
            // The linter checks all sources, which include ones that rely on generated classes.
            when (project.name) {
      //        "client" -> dependsOn("generateMainProtos")
              "service" -> dependsOn("generateProto")
              else -> {}
            }
            // Excludes generated sources. Add other source paths here as necessary.

            // Exclude commonly used location for generated sources
            exclude {
              it.file.absolutePath.contains("/generated/source/")
            }
          }
        }
      }

      subprojects {
      }
    """.trimIndent()

    val pluginFinder = PluginFinder.of(buildScript)

    assertThat(pluginFinder.plugins).isEqualTo(
      setOf(
        Plugin(Plugin.Type.APPLY, "polyrepo-plugin"),
        Plugin(Plugin.Type.APPLY, "org.jmailen.kotlinter"),
      )
    )
  }
}
