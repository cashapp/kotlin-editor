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
 * project declaration, an external module declaration, or a local file dependency. See [Type] and
 * [ModuleDependency](https://docs.gradle.org/current/javadoc/org/gradle/api/artifacts/ModuleDependency.html).
 */
public data class DependencyDeclaration(
  val configuration: String,
  val identifier: Identifier,
  val capability: Capability,
  val type: Type,
  val fullText: String,
  val precedingComment: String? = null,
) {

  public data class Identifier @JvmOverloads constructor(
    public val path: String,
    public val configuration: String? = null,
    public val explicitPath: Boolean = false,
  ) {

    /**
     * ```
     * 1. "g:a:v"
     * 2. path = "g:a:v"
     * 3. path = "g:a:v", configuration = "foo"
     * 4. "g:a:v", configuration = "foo"
     * ```
     */
    override fun toString(): String = buildString {
      if (explicitPath) {
        append("path = ")
      }

      append(path)

      if (configuration != null) {
        append(", configuration = ")
        append(configuration)
      }
    }

    internal companion object {
      fun String?.asSimpleIdentifier(): Identifier? {
        return if (this != null) {
          Identifier(path = this)
        } else {
          null
        }
      }
    }
  }

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
    FILE,
    FILES,
    FILE_TREE,
    MODULE,
    PROJECT,
    ;
  }
}
