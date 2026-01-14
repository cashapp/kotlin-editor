package cash.recipes.lint.buildscripts.utils

internal object BuildScripts {

  val noViolations = """
    plugins {
      id("foo")
      alias(libs.plugins.bar)
    }
  """.trimIndent()

  val hasViolations1 = """
      plugins {
        id("foo")
        alias(libs.plugins.bar)
      }
      
      dependencies {
        constraints {
          implementation("com.foo:bar") {
            version {
              require("1")
            }
          }
        }
      
        implementation(libs.foo)
        api("com.foo:bar:1.0")
        runtimeOnly(group = "foo", name = "bar", version = "2.0")
        compileOnly(group = "foo", name = "bar", version = libs.versions.bar.get()) {
          isTransitive = false
        }
        devImplementation(group = "io.netty", name = "netty-transport-native-kqueue", classifier = "osx-x86_64")
      }
      
      tasks {
        jar {
          archiveClassifier.set("unshaded")
        }
      }
      
      tasks.jar {
        archiveClassifier.set("unshaded")
      }
    """.trimIndent()

  val hasViolations2 = """
      plugins {
        id("foo")
        alias(libs.plugins.bar)
      }
      
      val foo = 1
    """.trimIndent()

  val hasViolations3 = """
      apply(plugin = "foo")
      
      val bar = 1
      
      @CacheableTask
      public class MyTask : DefaultTask() {
        @TaskAction fun action() {
          println("Ohnoes")
        }
      }
    """.trimIndent()
}
