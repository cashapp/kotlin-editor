package cash.grammar.kotlindsl.utils

import cash.grammar.kotlindsl.model.DependencyDeclaration
import cash.grammar.kotlindsl.model.DependencyDeclaration.Capability
import cash.grammar.kotlindsl.utils.Blocks.isBuildscript
import cash.grammar.kotlindsl.utils.Blocks.isDependencies
import cash.grammar.kotlindsl.utils.Context.leafRule
import cash.grammar.kotlindsl.utils.Context.literalText
import com.squareup.cash.grammar.KotlinParser.NamedBlockContext
import com.squareup.cash.grammar.KotlinParser.PostfixUnaryExpressionContext

public class DependencyExtractor {

  /**
   * Given that we're inside `buildscript { dependencies { ... } }`, collect all of the `classpath`
   * dependencies.
   *
   * Note that we don't actually validate that these are "classpath" dependencies, other than by
   * checking the block we're currently evaluating. This method should be generalizable to
   * collecting the set of dependencies from _any_ `dependencies` block.
   */
  public fun collectClasspathDependencies(
    blockStack: ArrayDeque<NamedBlockContext>,
    ctx: NamedBlockContext,
  ): List<DependencyDeclaration> {
    // Validate we're in `buildscript { dependencies { ... } }` first
    if (!isInBuildscriptDependenciesBlock(blockStack)) return emptyList()

    val statements = ctx.statements().statement()
    if (statements == null || statements.isEmpty()) return emptyList()

    return statements
      .mapNotNull { it.leafRule() as? PostfixUnaryExpressionContext }
      .map { parseDependencyDeclaration(it) }
  }

  private fun isInBuildscriptDependenciesBlock(
    blockStack: ArrayDeque<NamedBlockContext>,
  ): Boolean {
    return blockStack.size == 2
      && blockStack[0].isDependencies
      && blockStack[1].isBuildscript
  }

  private fun parseDependencyDeclaration(
    declaration: PostfixUnaryExpressionContext,
  ): DependencyDeclaration {
    // e.g., `classpath`, `implementation`, etc.
    val configuration = declaration.primaryExpression().text
    var identifier: String?
    var capability = Capability.DEFAULT
    var type = DependencyDeclaration.Type.MODULE

    // This is everything after the configuration, including optionally a trailing lambda
    val rawDependency = declaration.postfixUnarySuffix().single().callSuffix()

    /*
     * This leaf includes, in order:
     * 1. An optional "capability" (`platform` or `testFixtures`).
     * 2. An optional "project" prefix, indicating a project-type dependency.
     * 3. The dependency string, either to an external module or as a project path.
     * 4. An optional trailing lambda (configuration block)
     *
     * nb: This leaf is either a `LineStringLiteralContext` or a `PostfixUnaryExpressionContext`.
     *
     * It's a literal if it's a simple dependency defined as a `"group:artifact:version"` string.
     * It's a postfix... if it is anything more complex.
     */

    val leaf = rawDependency.valueArguments().valueArgument().single().leafRule()

    // 3. In case we have a simple dependency of the form `classpath("group:artifact:version")`
    identifier = literalText(leaf)

    // 1. Find capability, if present.
    // 2. Determine type, if present.
    if (leaf is PostfixUnaryExpressionContext) {
      val maybeCapability = leaf.primaryExpression().text
      if (Capability.isCapability(maybeCapability)) {
        capability = Capability.of(maybeCapability)

        identifier = literalText(
          leaf.postfixUnarySuffix().single()
            .callSuffix().valueArguments().valueArgument().single()
            .leafRule()
        )
      } else if (maybeCapability == "project") {
        type = DependencyDeclaration.Type.PROJECT

        identifier = literalText(
          leaf.postfixUnarySuffix().single()
            .callSuffix().valueArguments().valueArgument().single()
            .leafRule()
        )
      }

      // 2. Determine if `PROJECT` type.
      // 3. Also find `identifier` at this point.
      val newLeaf = leaf.postfixUnarySuffix().single()
        ?.callSuffix()?.valueArguments()?.valueArgument()?.single()
        ?.leafRule()

      if (newLeaf is PostfixUnaryExpressionContext) {
        val maybeProjectType = newLeaf.primaryExpression().text
        if (maybeProjectType == "project") {
          type = DependencyDeclaration.Type.PROJECT
          identifier = literalText(
            newLeaf.postfixUnarySuffix().single().callSuffix().valueArguments().valueArgument()
              .single()
          )
        } else {
          // e.g., `libs.kotlinGradleBom` ("libs" is the primaryExpression)
          identifier = newLeaf.text
        }
      } else if (newLeaf == null) {
        // We're looking at something like `libs.kotlinGradleBom`
        identifier = leaf.text
      }
    }

    return DependencyDeclaration(
      configuration = configuration,
      identifier = identifier!!,
      capability = capability,
      type = type,
    )
  }
}
