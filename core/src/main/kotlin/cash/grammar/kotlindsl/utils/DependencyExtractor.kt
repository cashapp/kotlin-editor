package cash.grammar.kotlindsl.utils

import cash.grammar.kotlindsl.model.*
import cash.grammar.kotlindsl.model.DependencyDeclaration.Capability
import cash.grammar.kotlindsl.model.DependencyDeclaration.Identifier
import cash.grammar.kotlindsl.model.DependencyDeclaration.Identifier.Companion.asSimpleIdentifier
import cash.grammar.kotlindsl.model.gradle.DependencyContainer
import cash.grammar.kotlindsl.utils.Blocks.isBuildscript
import cash.grammar.kotlindsl.utils.Blocks.isDependencies
import cash.grammar.kotlindsl.utils.Context.fullText
import cash.grammar.kotlindsl.utils.Context.leafRule
import cash.grammar.kotlindsl.utils.Context.literalText
import com.squareup.cash.grammar.KotlinParser.NamedBlockContext
import com.squareup.cash.grammar.KotlinParser.PostfixUnaryExpressionContext
import com.squareup.cash.grammar.KotlinParser.SimpleIdentifierContext
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext

public class DependencyExtractor(
  private val input: CharStream,
  tokens: CommonTokenStream,
  indent: String,
) {

  private val comments = Comments(tokens, indent)

  public fun onEnterBlock() {
    comments.onEnterBlock()
  }

  public fun onExitBlock() {
    comments.onExitBlock()
  }

  /**
   * Given that we're inside a `dependencies {}` block, collect the set of dependencies.
   */
  public fun collectDependencies(ctx: NamedBlockContext): DependencyContainer {
    require(ctx.isDependencies) {
      "Expected dependencies block. Was '${ctx.name().text}'"
    }

    val statements = ctx.statements().statement()
    if (statements.isNullOrEmpty()) return DependencyContainer.EMPTY

    return statements
      .map { stmt ->
        val leaf = stmt.leafRule()
        if (leaf is PostfixUnaryExpressionContext && leaf.isDependencyDeclaration()) {
          DependencyDeclarationElement(parseDependencyDeclaration(leaf), stmt)
        } else {
          NonDependencyDeclarationElement(stmt)
        }
      }
      .asContainer()
  }

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
  ): DependencyContainer {
    // Validate we're in `buildscript { dependencies { ... } }` first
    if (!isInBuildscriptDependenciesBlock(blockStack)) return DependencyContainer.EMPTY

    val statements = ctx.statements().statement()
    if (statements == null || statements.isEmpty()) return DependencyContainer.EMPTY

    return statements.mapNotNull { statement ->
      val leaf = statement.leafRule() as? PostfixUnaryExpressionContext ?: return@mapNotNull null
      if (!leaf.isDependencyDeclaration()) return@mapNotNull null
      DependencyDeclarationElement(parseDependencyDeclaration(leaf), statement)
    }.asContainer()
  }

  private fun List<DependencyElement>.asContainer() = DependencyContainer(this)

  private fun isInBuildscriptDependenciesBlock(
    blockStack: ArrayDeque<NamedBlockContext>,
  ): Boolean {
    return blockStack.size == 2
      && blockStack[0].isDependencies
      && blockStack[1].isBuildscript
  }

  private fun parseDependencyDeclaration(declaration: PostfixUnaryExpressionContext): DependencyDeclaration {
    // This is everything after the configuration, including optionally a trailing lambda
    val rawDependency = declaration.postfixUnarySuffix().single().callSuffix()
    val args = rawDependency.valueArguments().valueArgument()

    // e.g., `classpath`, `implementation`, etc.
    val configuration = declaration.primaryExpression().text
    var identifier: Identifier?
    var capability = Capability.DEFAULT
    var type = DependencyDeclaration.Type.MODULE

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

    val leaf = args.single().leafRule()

    // 3. In case we have a simple dependency of the form `classpath("group:artifact:version")`
    identifier = quoted(leaf).asSimpleIdentifier()

    // 1. Find capability, if present.
    // 2. Determine type, if present.
    if (leaf is PostfixUnaryExpressionContext) {
      val maybeCapability = leaf.primaryExpression().text
      if (Capability.isCapability(maybeCapability)) {
        capability = Capability.of(maybeCapability)

        identifier = quoted(
          leaf.postfixUnarySuffix().single()
            .callSuffix().valueArguments().valueArgument().single()
            .leafRule()
        ).asSimpleIdentifier()
      } else if (maybeCapability == "project") {
        type = DependencyDeclaration.Type.PROJECT

        // TODO(tsr): use findIdentifier everywhere?
        identifier = leaf.findIdentifier()
      } else if (maybeCapability == "file") {
        type = DependencyDeclaration.Type.FILE

        // TODO(tsr): use findIdentifier everywhere?
        identifier = leaf.findIdentifier()
      } else if (maybeCapability == "files") {
        type = DependencyDeclaration.Type.FILES

        // TODO(tsr): use findIdentifier everywhere?
        identifier = leaf.findIdentifier()
      } else if (maybeCapability == "fileTree") {
        type = DependencyDeclaration.Type.FILE_TREE

        // TODO(tsr): use findIdentifier everywhere?
        identifier = leaf.findIdentifier()
      } else if (maybeCapability != null) {
        identifier = leaf.findIdentifier(maybeCapability)
      }

      // 2. Determine if `PROJECT` type.
      // 3. Also find `identifier` at this point.
      val suffixes = leaf.postfixUnarySuffix()
      val args = suffixes.singleOrNull()?.callSuffix()?.valueArguments()?.valueArgument()

      if (args?.size == 1) {
        val newLeaf = args.single().leafRule()

        if (newLeaf is PostfixUnaryExpressionContext) {
          val maybeProjectType = newLeaf.primaryExpression().text
          if (maybeProjectType == "project") {
            type = DependencyDeclaration.Type.PROJECT
            identifier = newLeaf.findIdentifier()
          } else {
            // e.g., `libs.kotlinGradleBom` ("libs" is the primaryExpression)
            identifier = newLeaf.text.asSimpleIdentifier()
          }
        }
      } else if (args == null) {
        // We're looking at something like `libs.kotlinGradleBom`
        identifier = leaf.text.asSimpleIdentifier()
      }
    } else if (leaf is SimpleIdentifierContext) {
      // For expressions like `api(gav)` where `val gav = "..."`
      identifier = leaf.text.asSimpleIdentifier()
    }

    if (identifier == null && leaf is PostfixUnaryExpressionContext) {
      identifier = leaf.findIdentifier()
    }

    val precedingComment = comments.getCommentsToLeft(declaration)
    val fullText = declaration.fullText(input)
      ?: error("Could not determine 'full text' of dependency declaration. Failed to parse expression:\n  ${declaration.text}")

    return DependencyDeclaration(
      configuration = configuration,
      identifier = identifier
        ?: error("Could not determine dependency identifier. Failed to parse expression:\n  `$fullText`"),
      capability = capability,
      type = type.or(identifier),
      fullText = fullText,
      precedingComment = precedingComment,
    )
  }

  /**
   * The quotation marks are an important part of how the dependency is declared. Is it
   * ```
   * 1. "g:a:v", or
   * 2. libs.gav
   * ```
   * ?
   */
  private fun quoted(ctx: ParserRuleContext): String? {
    return literalText(ctx)?.let { "\"$it\"" }
  }

  private fun PostfixUnaryExpressionContext.isDependencyDeclaration(): Boolean {
    // This is everything after the configuration, including optionally a trailing lambda
    val rawDependency = this.postfixUnarySuffix().single().callSuffix()
    val args = rawDependency.valueArguments().valueArgument()

    // If there are more than one argument, it's a function call, not a dependency declaration
    return args.size <= 1
  }

  private fun PostfixUnaryExpressionContext.findIdentifier(): Identifier? {
    val args = postfixUnarySuffix().single()
      .callSuffix()
      .valueArguments()
      .valueArgument()

    // 1. possibly a simple identifier, like `g:a:v`, or
    // 2. `path = "foo"`
    if (args.size == 1) {
      val singleArg = args.single()

      quoted(singleArg.leafRule())?.let { identifier ->
        return Identifier(path = identifier)
      }

      // maybe `path = "foo"`
      val exprName = singleArg.simpleIdentifier()?.Identifier()?.text
      if (exprName == "path") {
        quoted(singleArg.expression().leafRule())?.let { identifier ->
          return Identifier(path = identifier, explicitPath = true)
        }
      }

      (singleArg.leafRule() as? SimpleIdentifierContext)?.Identifier()?.text.asSimpleIdentifier()?.let {
        return it
      }
    }

    // Unclear what this would be, bail
    if (args.size > 2) {
      return null
    }

    // possibly a map-like expression, e.g.,
    // 1. `path = "foo", configuration = "bar"`, or
    // 2. `"foo", configuration = "bar"`
    val firstArg = args[0]
    val secondArg = args[1]

    val firstKey = firstArg.simpleIdentifier()?.Identifier()?.text
    val secondKey = secondArg.simpleIdentifier()?.Identifier()?.text

    val firstValue = quoted(firstArg.expression()) ?: return null
    val secondValue = quoted(secondArg.expression()) ?: return null

    val path: String
    val configuration: String
    val explicitPath = firstKey == "path" || secondKey == "path"

    if (firstKey == "path" || firstKey == null) {
      require(secondKey == "configuration") {
        "Expected 'configuration', was '$secondKey'."
      }

      path = firstValue
      configuration = secondValue
    } else {
      require(firstKey == "configuration") {
        "Expected 'configuration', was '$firstKey'."
      }
      require(secondKey == "path") {
        "Expected 'path', was '$secondKey'."
      }

      path = secondValue
      configuration = firstValue
    }

    return Identifier(
      path = path,
      configuration = configuration,
      explicitPath = explicitPath,
    )
  }

  /**
   * Looking for something like this:
   * ```
   * gradleApi()
   * ```
   *
   * which is a proper dependency, to be used like this:
   * ```
   * dependencies {
   *   api(gradleApi())
   * }
   * ```
   */
  private fun PostfixUnaryExpressionContext.findIdentifier(name: String): Identifier? {
    val suffix = postfixUnarySuffix().singleOrNull() ?: return null
    val valueArguments = suffix.callSuffix()?.valueArguments() ?: return null
    // empty value arguments indicates "()", i.e., no arguments to the function call.
    if (valueArguments.valueArgument().isNotEmpty()) return null

    // e.g. "gradleApi()"
    return "$name()".asSimpleIdentifier()
  }
}
