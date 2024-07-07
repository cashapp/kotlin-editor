package cash.recipes.repos

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

internal class RepositoriesDeleterTest {

  // We don't want to delete the top-level `buildscript.repositories {}`
  @Test fun `can delete some repositories blocks`() {
    // Given a build script
    val buildScript =
      """
        buildscript {
          repositories {
            maven(url = "buildscript-repositories")
          }
          dependencies {
            classpath("com.group:artifact:1.0")
          }
        }
        
        plugins {
          id("java-library")
        }
        
        repositories {
          maven(url = "repositories")
        }
        
        // The buildscript and repositories blocks below should be completely deleted
        subprojects {
          apply(plugin = "java-library")
          
          buildscript {
            repositories {
              maven(url = "subprojects-buildscript-repositories")
            }
          }
          
          repositories {
            maven(url = "subprojects-repositories")
          }
        }
        
        allprojects {
          buildscript {
            repositories {
              maven(url = "allprojects-buildscript-repositories")
            }
          }
        }
      """.trimIndent()

    // When...
    val deleter = RepositoriesDeleter.of(buildScript)

    // Then...
    assertThat(deleter.rewritten()).isEqualTo(
      """
        buildscript {
          repositories {
            maven(url = "buildscript-repositories")
          }
          dependencies {
            classpath("com.group:artifact:1.0")
          }
        }
        
        plugins {
          id("java-library")
        }
        
        // The buildscript and repositories blocks below should be completely deleted
        subprojects {
          apply(plugin = "java-library")
        }
      """.trimIndent()
    )
  }

  @Test fun `can handle file annotations`() {
    // Given a build script
    val buildScript =
      """
        @file:Suppress("RemoveRedundantQualifierName")
        
        allprojects {
          buildscript {
            repositories {
              maven(url = "allprojects-buildscript-repositories")
            }
          }
        }
      """.trimIndent()

    // When...
    val deleter = RepositoriesDeleter.of(buildScript)

    // Then...
    assertThat(deleter.rewritten()).isEqualTo(
      """
        @file:Suppress("RemoveRedundantQualifierName")
      """.trimIndent()
    )
  }
}
