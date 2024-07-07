package cash.recipes.plugins

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class PluginNormalizerTest {

  @Test fun `can normalize plugin application`() {
    val buildScript =
      """
        plugins {
          id("com.squareup.wire")
          application
          id("buildSrc.foo-conventions")
          `kotlin-dsl`
          kotlin("plugin.serialization")
        }

        extension {
          foo = bar
        }

        apply(plugin = "kotlin")
        apply(plugin = "com.github.johnrengelman.shadow")
        apply(mapOf("plugin" to "foo"))
      """.trimIndent()

    val normalizer = PluginNormalizer.of(buildScript)

    assertThat(normalizer.rewritten()).isEqualTo(
      """
        plugins {
          id("com.squareup.wire")
          id("application")
          id("buildSrc.foo-conventions")
          id("kotlin-dsl")
          id("org.jetbrains.kotlin.plugin.serialization")
          id("org.jetbrains.kotlin.jvm")
          id("com.github.johnrengelman.shadow")
          id("foo")
        }
        
        extension {
          foo = bar
        }
      """.trimIndent()
    )
  }

  @Test fun `can add plugins block`() {
    val buildScript =
      """
        extension {
          foo = bar
        }

        apply(plugin = "kotlin")
        apply(plugin = "com.github.johnrengelman.shadow")
      """.trimIndent()

    val normalizer = PluginNormalizer.of(buildScript)

    assertThat(normalizer.rewritten()).isEqualTo(
      """
        plugins {
          id("org.jetbrains.kotlin.jvm")
          id("com.github.johnrengelman.shadow")
        }
        
        extension {
          foo = bar
        }
      """.trimIndent()
    )
  }

  @Test fun `can add plugins block 2`() {
    val buildScript =
      """
        apply(plugin = "kotlin")
        apply(plugin = "com.github.johnrengelman.shadow")
        
        extension {
          foo = bar
        }
      """.trimIndent()

    val normalizer = PluginNormalizer.of(buildScript)

    assertThat(normalizer.rewritten()).isEqualTo(
      """
        plugins {
          id("org.jetbrains.kotlin.jvm")
          id("com.github.johnrengelman.shadow")
        }
        
        
        extension {
          foo = bar
        }
      """.trimIndent()
    )
  }

  @Test fun `does nothing on an empty script`() {
    val buildScript = ""

    val normalizer = PluginNormalizer.of(buildScript)

    assertThat(normalizer.rewritten()).isEqualTo("")
  }

  @Test fun `does not eliminate too many newlines`() {
    // Given a build script...
    val buildScript = """
      plugins {
        id("com.squareup.wire")
        application
      }
      apply(plugin = "kotlin")
      apply(plugin = "com.github.johnrengelman.shadow")

      val protosSrc = "src/main/proto/"
    """.trimIndent()

    // When...
    val normalizedPluginScript = PluginNormalizer.of(buildScript).rewritten()

    // Then...
    assertThat(normalizedPluginScript).isEqualTo(
      """
        plugins {
          id("com.squareup.wire")
          id("application")
          id("org.jetbrains.kotlin.jvm")
          id("com.github.johnrengelman.shadow")
        }
        
        val protosSrc = "src/main/proto/"
      """.trimIndent()
    )
  }

  @Test fun `does not eliminate too many newlines 2`() {
    // Given a build script...
    val buildScript = """
        |
        |plugins {
        |    id("com.squareup.wire")
        |    id("com.squareup.cash.schemaregistry")
        |}
        |
        |apply(plugin = "kotlin")
        |
        |polyrepo {
        |  publishToArtifactory = true
        |  shortName = "balanceBasedAddCashClientProtos"
        |}
      """.trimMargin()

    // When...
    val normalizedPluginScript = PluginNormalizer.of(buildScript).rewritten()

    // Then...
    assertThat(normalizedPluginScript).isEqualTo(
      """
        |plugins {
        |    id("com.squareup.wire")
        |    id("com.squareup.cash.schemaregistry")
        |    id("org.jetbrains.kotlin.jvm")
        |}
        |polyrepo {
        |  publishToArtifactory = true
        |  shortName = "balanceBasedAddCashClientProtos"
        |}
      """.trimMargin()
    )
  }

  @Test fun `retains version strings and apply config, infix or not`() {
    // Given a build script...
    val buildScript = """
      plugins {
        kotlin("plugin.serialization") version "1.6.10"
        kotlin("jvm") version "1.6.10" apply false
        kotlin("kapt").version(libs.kotlin.get().version)
        id("my-versioned-plugin").version("x")
        id("my-unapplied-versioned-plugin").version("x").apply(false)
        id("my-unapplied-plugin").apply(false)
      }
    """.trimIndent()

    // When...
    val normalizedPluginScript = PluginNormalizer.of(buildScript).rewritten()

    // Then...
    assertThat(normalizedPluginScript).isEqualTo(
      """
        plugins {
          id("org.jetbrains.kotlin.plugin.serialization") version "1.6.10"
          id("org.jetbrains.kotlin.jvm") version "1.6.10" apply false
          id("org.jetbrains.kotlin.kapt") version libs.kotlin.get().version
          id("my-versioned-plugin") version "x"
          id("my-unapplied-versioned-plugin") version "x" apply false
          id("my-unapplied-plugin") apply false
        }
      """.trimIndent()
    )
  }

  @Test fun `insert after importList`() {
    // Given a build script...
    // cash-balance-based-add-cash/build.gradle.kts
    val buildScript = """
      import com.squareup.cash.kripper.plugin.gradle.KripperPluginExtension
      import com.squareup.polyrepo.plugin.PolyrepoExtension
      
      apply(plugin = "cash-kripper")
      
      allprojects {
        apply(plugin = "polyrepo-plugin")
      }
    """.trimIndent()

    // When...
    val normalizedPluginScript = PluginNormalizer.of(buildScript).rewritten()

    // Then...
    assertThat(normalizedPluginScript).isEqualTo(
      """
        import com.squareup.cash.kripper.plugin.gradle.KripperPluginExtension
        import com.squareup.polyrepo.plugin.PolyrepoExtension
        
        plugins {
          id("cash-kripper")
        }
        
        
        allprojects {
          apply(plugin = "polyrepo-plugin")
        }
      """.trimIndent()
    )
  }

  @Test fun `insert after buildscript block`() {
    // Given a build script...
    // cash-balance-based-add-cash/build.gradle.kts
    val buildScript = """
      buildscript {
        repositories {
          maven(url = "https://maven.global.square/artifactory/square-public")
        }
      
        dependencies {
          classpath(libs.cashKripper)
          classpath(libs.cashDevSourcesPlugin)
          classpath(libs.junitGradlePlugin)
          classpath(libs.ktranslatePlugin)
          classpath(platform(libs.kotlinGradleBom))
          classpath(libs.kotlinGradlePlugin)
          classpath(libs.cashPolyrepoPlugin)
          classpath(libs.redactedCompilerPluginGradle)
          classpath(libs.shadowJarPlugin)
          classpath(platform(libs.cashCloudUberBom))
          classpath(libs.wireGradlePlugin)
          classpath(libs.schemaRegistryPlugin)
        }
      }
      
      apply(plugin = "cash-kripper")
      
      allprojects {
        apply(plugin = "polyrepo-plugin")
      }
    """.trimIndent()

    // When...
    val normalizedPluginScript = PluginNormalizer.of(buildScript).rewritten()

    // Then...
    assertThat(normalizedPluginScript).isEqualTo(
      """
        buildscript {
          repositories {
            maven(url = "https://maven.global.square/artifactory/square-public")
          }
        
          dependencies {
            classpath(libs.cashKripper)
            classpath(libs.cashDevSourcesPlugin)
            classpath(libs.junitGradlePlugin)
            classpath(libs.ktranslatePlugin)
            classpath(platform(libs.kotlinGradleBom))
            classpath(libs.kotlinGradlePlugin)
            classpath(libs.cashPolyrepoPlugin)
            classpath(libs.redactedCompilerPluginGradle)
            classpath(libs.shadowJarPlugin)
            classpath(platform(libs.cashCloudUberBom))
            classpath(libs.wireGradlePlugin)
            classpath(libs.schemaRegistryPlugin)
          }
        }
        
        plugins {
          id("cash-kripper")
        }
        
        
        allprojects {
          apply(plugin = "polyrepo-plugin")
        }
      """.trimIndent()
    )
  }

  @Test fun `duplicates are not included`() {
    // Given a build script with duplicate plugins
    val buildScript = """
      plugins {
        id("org.jetbrains.kotlin.jvm")
        id("com.squareup.cash.ktranslate")
        id("com.squareup.wire")
        id("dev.zacsweers.redacted")
        id("cash-kripper")
        id("org.jetbrains.kotlinx.kover")
        id("io.gitlab.arturbosch.detekt")
        id("com.squareup.cash.promoter.notification-builder-plugin")
        application
      }
      apply(plugin = "kotlin")
      apply(plugin = "com.github.johnrengelman.shadow")
      apply(plugin = "cash-kripper")
      apply(plugin = "io.gitlab.arturbosch.detekt")
    """.trimIndent()

    // When...
    val normalizedPluginScript = PluginNormalizer.of(buildScript).rewritten()

    // Then...
    assertThat(normalizedPluginScript).isEqualTo(
      """
        plugins {
          id("org.jetbrains.kotlin.jvm")
          id("com.squareup.cash.ktranslate")
          id("com.squareup.wire")
          id("dev.zacsweers.redacted")
          id("cash-kripper")
          id("org.jetbrains.kotlinx.kover")
          id("io.gitlab.arturbosch.detekt")
          id("com.squareup.cash.promoter.notification-builder-plugin")
          id("application")
          id("com.github.johnrengelman.shadow")
        }
      """.trimIndent()
    )
  }

  @Test fun `handles various kotlin plugins by apply`() {
    // Given Kotlin nonsense...
    val buildScript = """
      apply(plugin = "kotlin")
      apply(plugin = "kotlin-kapt")
      apply(plugin = "kotlin-allopen")
      apply(plugin = "kotlin-jpa")
      apply(plugin = "kotlinx-serialization")
    """.trimIndent()

    // When...
    val normalizedPluginScript = PluginNormalizer.of(buildScript).rewritten()

    // Then...
    assertThat(normalizedPluginScript).isEqualTo(
      """
        plugins {
          id("org.jetbrains.kotlin.jvm")
          id("org.jetbrains.kotlin.kapt")
          id("org.jetbrains.kotlin.plugin.allopen")
          id("org.jetbrains.kotlin.plugin.jpa")
          id("org.jetbrains.kotlin.plugin.serialization")
        }
      """.trimIndent()
    )
  }

  @Test fun `handles various kotlin plugins by kotlin-block`() {
    // Given Kotlin nonsense...
    val buildScript = """
      plugins {
        kotlin("jvm")
        kotlin("kapt")
        kotlin("plugin.allopen")
        kotlin("plugin.jpa")
        kotlin("plugin.serialization")
      }
    """.trimIndent()

    // When...
    val normalizedPluginScript = PluginNormalizer.of(buildScript).rewritten()

    // Then...
    assertThat(normalizedPluginScript).isEqualTo(
      """
        plugins {
          id("org.jetbrains.kotlin.jvm")
          id("org.jetbrains.kotlin.kapt")
          id("org.jetbrains.kotlin.plugin.allopen")
          id("org.jetbrains.kotlin.plugin.jpa")
          id("org.jetbrains.kotlin.plugin.serialization")
        }
      """.trimIndent()
    )
  }
}
