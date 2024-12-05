package cash.recipes.dependencies

import cash.recipes.dependencies.transform.Transform
import cash.recipes.dependencies.transform.Transform.Element
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class DependenciesMutatorTest {

  @Test fun `can remap dependency declarations`() {
    // Given a build script and a map of changes
    val expectedUsedTransforms = listOf(
      // Can handle just the identifier (no version)
      Transform(
        from = Element.StringLiteral("com.foo:bar"),
        to = Element.Accessor("libs.fooBar"),
      ),
      // Can handle full GAV coordinates (including version)
      Transform(
        from = Element.StringLiteral("group:artifact:1.0"),
        to = Element.Accessor("libs.artifact"),
      ),
    )
    val transforms = expectedUsedTransforms +
      // Doesn't find this dependency, so nothing happens
      Transform(
        from = Element.StringLiteral("org.magic:turtles"),
        to = Element.Accessor("libs.magicTurtles"),
      )

    val buildScript = """
      dependencies {
        api("com.foo:bar:1.0")
        implementation(libs.foo)
        runtimeOnly("group:artifact:1.0") { isTransitive = false }
      }
    """.trimIndent()

    // When
    val mutator = DependenciesMutator.of(buildScript, transforms)
    val rewrittenContent = mutator.rewritten()

    // Then
    assertThat(rewrittenContent).isEqualTo(
      """
        dependencies {
          api(libs.fooBar)
          implementation(libs.foo)
          runtimeOnly(libs.artifact) { isTransitive = false }
        }
      """.trimIndent()
    )

    // ...and we can get the list of transforms
    val usedTransforms = mutator.usedTransforms()
    assertThat(usedTransforms).containsExactlyElementsOf(expectedUsedTransforms)
  }
}
