package cash.grammar.kotlindsl.utils

import cash.grammar.kotlindsl.model.DependencyDeclaration
import cash.grammar.kotlindsl.model.DependencyDeclaration.Capability
import cash.grammar.kotlindsl.model.DependencyDeclaration.Identifier
import cash.grammar.kotlindsl.model.DependencyDeclaration.Identifier.Companion.asSimpleIdentifier
import cash.grammar.kotlindsl.model.DependencyDeclaration.Identifier.IdentifierElement
import cash.grammar.kotlindsl.model.DependencyDeclarationElement
import cash.grammar.kotlindsl.model.DependencyElement
import cash.grammar.kotlindsl.model.NonDependencyDeclarationElement
import cash.grammar.kotlindsl.model.gradle.DependencyContainer
import cash.grammar.kotlindsl.utils.Blocks.isBuildscript
import cash.grammar.kotlindsl.utils.Blocks.isDependencies
import cash.grammar.kotlindsl.utils.Context.fullText
import cash.grammar.kotlindsl.utils.Context.leafRule
import cash.grammar.kotlindsl.utils.Context.literalText
import com.squareup.cash.grammar.KotlinParser.NamedBlockContext
import com.squareup.cash.grammar.KotlinParser.PostfixUnaryExpressionContext
import com.squareup.cash.grammar.KotlinParser.SimpleIdentifierContext
import com.squareup.cash.grammar.KotlinParser.ValueArgumentContext
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext

public class DependencyExtractor(
  private val input: CharStream,
  tokens: CommonTokenStream,
  indent: String,
) {

  private enum class DeclarationDetectionResult {
    DECLARATION_NORMAL,  // like `implementation("...") { ... }`
    DECLARATION_COMPLEX, // like `implementation(group = "", name = "", version = "", configuration = "", classifier = "", ext = "") { ... }`
    STATEMENT,           // not an obvious dependency declaration
    ;
  }

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
        if (leaf is PostfixUnaryExpressionContext) {
          when (leaf.isDependencyDeclaration()) {
            DeclarationDetectionResult.DECLARATION_NORMAL -> {
              DependencyDeclarationElement(parseDependencyDeclaration(leaf), stmt)
            }

            DeclarationDetectionResult.DECLARATION_COMPLEX -> {
              DependencyDeclarationElement(parseComplexDependencyDeclaration(leaf), stmt)
            }

            else -> NonDependencyDeclarationElement(stmt)
          }
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
      // TODO(tsr): handle complex declaration?
      if (leaf.isDependencyDeclaration() != DeclarationDetectionResult.DECLARATION_NORMAL) return@mapNotNull null
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

  /**
   * Parses a [DependencyDeclaration] out of a [declaration] in source code.
   *
   * @see [parseComplexDependencyDeclaration]
   */
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
      producerConfiguration = identifier.configuration,
      capability = capability,
      type = type.or(identifier),
      fullText = fullText,
      precedingComment = precedingComment,
    )
  }

  /**
   * Given a declaration like
   * ```
   * implementation(group = "...", name = "...", version = ..., configuration = "...", classifier = "...", ext = "...") {
   *   ...
   * }
   * ```
   * ...where `version` can be either a String literal or a complex expression, for example
   * ```
   * version = "1.0" // 1
   * version = libs.versions.foo.get() // 2
   * ```
   *
   * Will return a [DependencyDeclaration].
   *
   * TODO(tsr): this method can only handle the simplest case of a "complex declaration" on a module dependency with
   *  the default capability.
   *
   * @see [parseDependencyDeclaration]
   */
  private fun parseComplexDependencyDeclaration(declaration: PostfixUnaryExpressionContext): DependencyDeclaration {
    // This is everything after the configuration, including optionally a trailing lambda
    val rawDependency = declaration.postfixUnarySuffix().single().callSuffix()
    val args = rawDependency.valueArguments().valueArgument()

    // e.g., `classpath`, `implementation`, etc.
    val configuration = declaration.primaryExpression().text
    val capability = Capability.DEFAULT
    val type = DependencyDeclaration.Type.MODULE

    fun List<ValueArgumentContext>.valueOf(name: String): IdentifierElement? {
      val expression = firstOrNull { it.simpleIdentifier().text == name }?.expression()
      var value = expression?.let { literalText(it) }
      val isString = value != null

      // only `version` is permitted to not be a string literal
      if (value == null && name == "version") {
        value = expression?.text
      }

      return value?.let {
        IdentifierElement(
          value = value,
          isStringLiteral = isString,
        )
      }
    }

    val group = args.valueOf("group") ?: error("missing group")
    val name = args.valueOf("name") ?: error("missing name")
    val version = args.valueOf("version")
    val classifier = args.valueOf("classifier")
    val ext = args.valueOf("ext")
    val producerConfiguration = args.valueOf("configuration")

    val identifier = if (version == null) {
      "\"${group.value}:${name.value}\"".asSimpleIdentifier()
    } else if (version.isStringLiteral) {
      "\"${group.value}:${name.value}:${version.value}\"".asSimpleIdentifier()
    } else {
      // version is non-null and a complex expression (not a string literal)
      "\"${group.value}:${name.value}:\${${version.value}}\"".asSimpleIdentifier()
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
      producerConfiguration = producerConfiguration?.value,
      classifier = classifier?.value,
      ext = ext?.value,
      fullText = fullText,
      precedingComment = precedingComment,
      isComplex = true,
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

  private fun PostfixUnaryExpressionContext.isDependencyDeclaration(): DeclarationDetectionResult {
    // Something strange
    if (postfixUnarySuffix().size != 1) return DeclarationDetectionResult.STATEMENT

    // This is everything after the configuration, including optionally a trailing lambda
    val rawDependency = postfixUnarySuffix().single().callSuffix()
    // Probably not actually a dependencies block
    val valueArguments = rawDependency.valueArguments() ?: return DeclarationDetectionResult.STATEMENT
    val args = valueArguments.valueArgument()

    // If there are more than one argument, it's a function call, not a dependency declaration
    return if (args.size <= 1) {
      DeclarationDetectionResult.DECLARATION_NORMAL
    } else if (looksLikeComplexDeclaration(args)) {
      DeclarationDetectionResult.DECLARATION_COMPLEX
    } else {
      DeclarationDetectionResult.STATEMENT
    }
  }

  /**
   * For example:
   * ```
   * implementation(group = "", name = "", version = "", configuration = "", classifier = "", ext = "") { ... }
   * ```
   */
  private fun looksLikeComplexDeclaration(argContexts: List<ValueArgumentContext>): Boolean {
    // max number of args
    if (argContexts.size > 6) return false
    val args = argContexts.mapNotNull { it.simpleIdentifier()?.text }

    // Then some of our args don't match the expected form of `name = value`
    if (args.size != argContexts.size) return false

    val requiredArgs = setOf("group", "name")

    // `group` and `name` are required arguments
    if (!args.containsAll(requiredArgs)) return false

    val validArgNames = listOf("group", "name", "version", "configuration", "classifier", "ext")
    // We need a mutable list because each name can only be used once.
    val argNames = validArgNames.toMutableList()

    // Every arg in args must have a match in validArgNames AND `group` and `name` are required arguments
    return args.all { argName -> argNames.remove(argName) }
  }

  private fun PostfixUnaryExpressionContext.findIdentifier(): Identifier? {
    val args = postfixUnarySuffix().single()
      .callSuffix()
      .valueArguments()
      .valueArgument()

    // A declaration like `project()` (meaning _this_ project, the main source).
    if (args.isEmpty()) {
      return if (text == "project()") {
        text.asSimpleIdentifier()
      } else {
        // Unclear what this is
        error("Unknown type of dependency declaration. Text = `${fullText(input)}`")
      }
    }

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
    if (args.size != 2) {
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
