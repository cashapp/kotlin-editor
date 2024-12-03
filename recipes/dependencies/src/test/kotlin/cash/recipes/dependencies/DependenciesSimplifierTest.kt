package cash.recipes.dependencies

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class DependenciesSimplifierTest {

  @Test fun `can simplify dependency declarations`() {
    // Given
    val buildScript = """
      dependencies {
        implementation(libs.foo)
        api("com.foo:bar:1.0")
        runtimeOnly(group = "foo", name = "bar", version = "2.0")
        compileOnly(group = "foo", name = "bar", version = libs.versions.bar.get()) {
          isTransitive = false
        }
        devImplementation(group = "io.netty", name = "netty-transport-native-kqueue", classifier = "osx-x86_64")
      }
    """.trimIndent()

    // When
    val simplifier = DependenciesSimplifier.of(buildScript)
    val rewrittenContent = simplifier.rewritten()

    // Then all declarations are simplified
    assertThat(rewrittenContent).isEqualTo(
      """
        dependencies {
          implementation(libs.foo)
          api("com.foo:bar:1.0")
          runtimeOnly("foo:bar:2.0")
          compileOnly("foo:bar:${'$'}{libs.versions.bar.get()}") {
            isTransitive = false
          }
          devImplementation(group = "io.netty", name = "netty-transport-native-kqueue", classifier = "osx-x86_64")
        }
      """.trimIndent()
    )
  }
}
