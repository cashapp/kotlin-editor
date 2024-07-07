package cash.grammar.kotlindsl.model

/**
 * Example dependency declarations:
 * ```
 * // External module dependencies
 * classpath(libs.foo)
 * classpath(platform(libs.foo))
 * classpath(testFixtures(libs.foo))
 *
 * // Project dependencies
 * classpath(project(":path"))
 * classpath(platform(project(":path")))
 * classpath(testFixtures(project(":path")))
 * ```
 *
 * Where "classpath" could be any [configuration] name whatsoever, including (for example; this is not
 * exhaustive):
 * 1. `classpath`
 * 2. `implementation`
 * 3. `api`
 * 4. `testRuntimeOnly`
 * 5. etc. etc.
 *
 * [identifier] corresponds to the name or base coordinates of the dependency, e.g.:
 * 1. libs.foo
 * 2. ":path"
 *
 * [capability] corresponds to the dependency's
 * [capability](https://docs.gradle.org/current/userguide/component_capabilities.html), in Gradle
 * terms, and can be one of three kinds. See [Capability].
 *
 * Finally we have [type], which tells us whether this dependency declaration is for an internal
 * project declaration or an external module declaration. See [Type] and
 * [ModuleDependency](https://docs.gradle.org/current/javadoc/org/gradle/api/artifacts/ModuleDependency.html).
 */
public data class DependencyDeclaration(
  val configuration: String,
  val identifier: String,
  val capability: Capability,
  val type: Type,
) {

  /**
   * @see <a href="https://docs.gradle.org/current/userguide/component_capabilities.html">Component Capabilities</a>
   */
  public enum class Capability {
    DEFAULT,
    PLATFORM,
    TEST_FIXTURES,
    ;

    public companion object {
      private val capabilities = listOf("testFixtures", "platform")

      public fun isCapability(value: String): Boolean = value in capabilities

      public fun of(value: String): Capability {
        return when (value) {
          "testFixtures" -> TEST_FIXTURES
          "platform" -> PLATFORM
          else -> error(
            "Unrecognized capability: '$value'. Expected one of '$capabilities'."
          )
        }
      }
    }
  }

  /**
   * @see <a href="https://docs.gradle.org/current/javadoc/org/gradle/api/artifacts/ModuleDependency.html">ModuleDependency</a>
   */
  public enum class Type {
    PROJECT,
    MODULE,
    ;
  }
}
