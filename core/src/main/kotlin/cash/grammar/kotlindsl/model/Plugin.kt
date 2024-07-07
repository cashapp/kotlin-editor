package cash.grammar.kotlindsl.model

public data class Plugin(
  public val type: Type,
  public val id: String,
  public val version: String? = null,
  public val applied: Boolean = true,
) {

  public fun asIdString(): String? {
    return when (type) {
      Type.BLOCK_SIMPLE -> id
      Type.BLOCK_BACKTICK -> "`$id`"
      Type.BLOCK_KOTLIN -> "kotlin(\"$id\")"
      Type.BLOCK_ID -> "id(\"$id\")"
      Type.BLOCK_ALIAS -> "alias($id)"
      Type.APPLY -> null // Not part of plugin block
    }
  }

  public enum class Type {
    /**
     * Plugin was applied to a script like
     * ```
     * apply(plugin = "a-plugin")
     * ```
     * or
     * ```
     * apply(mapOf("plugin" to "a-plugin"))
     * ```
     *
     * It's [Plugin.id] in either case would be `"a-plugin"`.
     */
    APPLY,

    /**
     * Plugin was applied to a script like
     * ```
     * plugins {
     *   alias(libs.plugins.by.alias)
     * }
     * ```
     * It's [Plugin.id] in this case would be `"libs.plugins.by.alias"`.
     */
    BLOCK_ALIAS,

    /**
     * Plugin was applied to a script like
     * ```
     * plugins {
     *   `kotlin-dsl`
     * }
     * ```
     * It's [Plugin.id] in this case would be `kotlin-dsl`.
     */
    BLOCK_BACKTICK,

    /**
     * Plugin was applied to a script like
     * ```
     * plugins {
     *   id("a-plugin")
     * }
     * ```
     * It's [Plugin.id] in this case would be `"a-plugin"`.
     */
    BLOCK_ID,

    /**
     * Plugin was applied to a script like
     * ```
     * plugins {
     *   kotlin("jvm")
     * }
     * ```
     * It's [Plugin.id] in this case would be `"jvm"`.
     */
    BLOCK_KOTLIN,

    /**
     * Plugin was applied to a script like
     * ```
     * plugins {
     *   application
     * }
     * ```
     * It's [Plugin.id] in this case would be `application`.
     */
    BLOCK_SIMPLE,
    ;

    public companion object {
      /**
       * Returns the plugin "type" from [value]. Available types are:
       * * [APPLY] -> Plugins that are applied via `apply(plugin = "...")`
       * * [BLOCK_ID] -> Plugins that are applied like `plugins { id("...") }`
       * * [BLOCK_KOTLIN] -> Kotlin plugins that are applied like `plugins { kotlin("jvm") }`
       *
       * Note that the following cannot be produced via this method:
       * * [BLOCK_BACKTICK] -> Plugins that are applied like `plugins { `kotlin-dsl` }`
       * * [BLOCK_SIMPLE] -> Core plugins that are applied like `plugins { application }`
       *
       * [PluginExtractor][cash.grammar.kotlindsl.utils.PluginExtractor] can create valid types
       * in the right parser rule contexts.
       */
      public fun of(value: String): Type {
        return when (value) {
          "apply" -> APPLY
          "alias" -> BLOCK_ALIAS
          "id" -> BLOCK_ID
          "kotlin" -> BLOCK_KOTLIN
          // TODO BLOCK_BACKTICK, BLOCK_SIMPLE
          else -> {
            val supportedTypes = listOf("apply", "alias", "id", "kotlin")
            error("Unknown plugin type. Was '$value'. Expected one of $supportedTypes")
          }
        }
      }
    }
  }
}
