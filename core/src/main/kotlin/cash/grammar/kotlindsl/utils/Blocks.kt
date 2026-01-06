package cash.grammar.kotlindsl.utils

import com.squareup.cash.grammar.KotlinParser.NamedBlockContext
import com.squareup.cash.grammar.KotlinParser.ScriptContext
import com.squareup.cash.grammar.KotlinParser.StatementContext
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.TerminalNode

public object Blocks {

  public const val ALLPROJECTS: String = "allprojects"
  public const val BUILDSCRIPT: String = "buildscript"
  public const val DEPENDENCIES: String = "dependencies"
  public const val DEPENDENCY_RESOLUTION_MANAGEMENT: String = "dependencyResolutionManagement"
  public const val PLUGINS: String = "plugins"
  public const val REPOSITORIES: String = "repositories"
  public const val SUBPROJECTS: String = "subprojects"

  public val NamedBlockContext.isAllprojects: Boolean
    get() = name().text == ALLPROJECTS

  public val NamedBlockContext.isBuildscript: Boolean
    get() = name().text == BUILDSCRIPT

  public val NamedBlockContext.isDependencies: Boolean
    get() = name().text == DEPENDENCIES

  public val NamedBlockContext.isDependencyResolutionManagement: Boolean
    get() = name().text == DEPENDENCY_RESOLUTION_MANAGEMENT

  /**
   * Returns true if this is the top-level "plugins" block. False otherwise. In particular, will
   * return false in this case:
   * ```
   * gradlePlugin {
   *   plugins { ... } // not the "plugins" block
   * }
   * ```
   */
  public val NamedBlockContext.isPlugins: Boolean
    get() = name().text == PLUGINS && isTopLevel(this)

  public val NamedBlockContext.isRepositories: Boolean
    get() = name().text == REPOSITORIES

  public val NamedBlockContext.isSubprojects: Boolean
    get() = name().text == SUBPROJECTS

  /**
   * Returns the outermost block relative to the current block, represented by the block at the top
   * of the [stack]. This outermost block must only contain a single block (which itself can contain
   * a single block...), and nothing else. For example, given this (where `repositories` is at the
   * top of the stack):
   *
   * ```
   * subprojects {
   *   buildscript {
   *     repositories { ... }
   *   }
   * }
   * ```
   * This function will return the `subprojects {}` block.
   *
   * However, given this (again, where `repositories` is at the top of the stack):
   * ```
   * subprojects {
   *   apply(plugin = "...")
   *
   *   buildscript {
   *     repositories { ... }
   *   }
   * }
   * ```
   * This function will return the `buildscript {}` block.
   */
  public fun getOutermostBlock(
    stack: ArrayDeque<NamedBlockContext>
  ): NamedBlockContext? {
    if (stack.isEmpty()) return null
    if (stack.size == 1) return stack.first()

    // Current, innermost block
    var outermost = stack[0]
    var index = 1
    var parent = stack[index]

    // terminal nodes are lexical tokens, not parse rule contexts
    var realNodes = parent.children.filterNot { it is TerminalNode }

    // Direct parent has more than just _this_ child, so return this child (current block)
    if (realNodes.size != 2 || realNodes[1].childCount != 2) return outermost

    outermost = parent

    while (realNodes.size == 2 && ++index < stack.size) {
      parent = stack[index]
      realNodes = parent.children.filterNot { it is TerminalNode }

      if (realNodes.size == 2 && realNodes[1].childCount == 2) {
        outermost = parent
      } else {
        break
      }
    }

    return outermost
  }

  /**
   * If [name] is null (default), returns true if [ctx] is within _any_ named block. Otherwise, only
   * returns true if [ctx] is within block whose name equals [name].
   *
   * Given
   * ```
   * foo = bar
   * ```
   * This method would return `false`.
   *
   * Given
   * ```
   * foo {
   *   bar = baz
   * }
   * ```
   * This method would return `true`.
   */
  public fun isInNamedBlock(ctx: ParserRuleContext, name: String? = null): Boolean {
    return enclosingNamedBlock(ctx, name) != null
  }

  /** The inverse of [isInNamedBlock]. */
  public fun isNotInNamedBlock(ctx: ParserRuleContext, name: String? = null): Boolean {
    return !isInNamedBlock(ctx, name)
  }

  /**
   * Essentially an alias for [isNotInNamedBlock]. Returns true if [ctx] is at the top level of a
   * build script (not nested in another block).
   */
  public fun isTopLevel(ctx: ParserRuleContext): Boolean {
    return isNotInNamedBlock(ctx, null)
  }

  /**
   * If [name] is null (default), returns name of enclosing named block if [ctx] is within _any_
   * named block. Otherwise, returns [name] if [ctx] is within block whose name equals [name].
   *
   * Given
   * ```
   * foo = bar
   * ```
   * This method would return `null`.
   *
   * Given
   * ```
   * foo {
   *   bar = baz
   * }
   * ```
   * This method would return `"foo"`.
   */
  public fun enclosingNamedBlock(ctx: ParserRuleContext, name: String? = null): String? {
    var parent = ctx.parent
    while (parent !is ScriptContext) {
      if (parent is NamedBlockContext) {
        val parentName = parent.name().text
        if (name == null || parentName == name) {
          return parentName
        }
      }

      parent = parent.parent
    }

    return null
  }

  /**
   * Iterates over all named blocks in [iter], filtering by [name] if it is not null.
   * For each named block, calls [action] with the block as the argument. Note that this is not a recursive
   * iteration; only direct elements of [iter] are considered.
   */
  public fun forEachNamedBlock(
    iter: Iterable<StatementContext>,
    name: String? = null,
    action: (NamedBlockContext) -> Unit
  ) {
    iter.mapNotNull { it.namedBlock() }
      .filter { name == null || it.name().text == name }
      .forEach { action(it) }
  }
}
