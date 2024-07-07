package cash.recipes.repos

import cash.recipes.repos.exception.AlreadyHasBlockException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

internal class DependencyResolutionManagementAdderTest {

  @Test fun `can add drm block after the plugins block`() {
    // Given a settings script
    val settingsScript =
      """
        rootProject.name = "sample-project"
        
        pluginManagement {
          repositories {
            mavenCentral()
          }
        }
        
        plugins {
          id("java-library")
        }
      """.trimIndent()

    // When...
    val adder = DependencyResolutionManagementAdder.of(settingsScript, listOf("mavenCentral()"))

    // Then...
    assertThat(adder.rewritten()).isEqualTo(
      """
        rootProject.name = "sample-project"
        
        pluginManagement {
          repositories {
            mavenCentral()
          }
        }
        
        plugins {
          id("java-library")
        }
        
        dependencyResolutionManagement {
          repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
          repositories {
            mavenCentral()
          }
        }
      """.trimIndent()
    )
  }

  @Test fun `can add drm block to script without a plugins block`() {
    // Given a settings script
    val settingsScript =
      """
        rootProject.name = "sample-project"
        
        pluginManagement {
          repositories {
            mavenCentral()
          }
        }
      """.trimIndent()

    // When...
    val adder = DependencyResolutionManagementAdder.of(settingsScript, listOf("mavenCentral()"))

    // Then...
    assertThat(adder.rewritten()).isEqualTo(
      """
        rootProject.name = "sample-project"
        
        pluginManagement {
          repositories {
            mavenCentral()
          }
        }
        
        dependencyResolutionManagement {
          repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
          repositories {
            mavenCentral()
          }
        }
      """.trimIndent()
    )
  }

  @Test fun `can add drm block to an empty script`() {
    // Given a settings script
    val settingsScript = ""

    // When...
    val adder = DependencyResolutionManagementAdder.of(settingsScript, listOf("mavenCentral()"))

    // Then...
    assertThat(adder.rewritten()).isEqualTo(
      """
        dependencyResolutionManagement {
          repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
          repositories {
            mavenCentral()
          }
        }
      """.trimIndent()
    )
  }

  @Test fun `throws if drm block already present`() {
    // Given a settings script
    val settingsScript =
      """
        dependencyResolutionManagement {}
      """.trimIndent()

    // When...
    val adder = DependencyResolutionManagementAdder.of(settingsScript, listOf("mavenCentral()"))

    // Then...
    assertThrows(AlreadyHasBlockException::class.java) {
      adder.rewritten()
    }
  }

  @Test fun `throws if no repos passed in`() {
    // Given a settings script
    val settingsScript = ""

    // Expect...
    assertThrows(IllegalStateException::class.java) {
      DependencyResolutionManagementAdder.of(settingsScript, listOf())
    }
  }
}
