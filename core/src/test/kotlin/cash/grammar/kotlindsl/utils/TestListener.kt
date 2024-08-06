package cash.grammar.kotlindsl.utils

import cash.grammar.kotlindsl.model.DependencyDeclaration
import cash.grammar.kotlindsl.utils.Blocks.isBuildscript
import cash.grammar.kotlindsl.utils.Blocks.isDependencies
import cash.grammar.kotlindsl.utils.Blocks.isSubprojects
import com.squareup.cash.grammar.KotlinParser
import com.squareup.cash.grammar.KotlinParserBaseListener
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.Token

internal class TestListener(
  private val input: CharStream,
  private val tokens: CommonTokenStream,
  private val defaultIndent: String = "  ",
) : KotlinParserBaseListener() {

  var newlines: List<Token>? = null
  var whitespace: List<Token>? = null
  val trailingBuildscriptNewlines = Whitespace.countTerminalNewlines(tokens)
  val trailingKotlinFileNewlines = Whitespace.countTerminalNewlines(tokens)
  val indent = Whitespace.computeIndent(tokens, input, defaultIndent)
  val dependencyExtractor = DependencyExtractor(input, tokens, indent)

  val dependencyDeclarations = mutableListOf<DependencyDeclaration>()

  override fun exitNamedBlock(ctx: KotlinParser.NamedBlockContext) {
    if (ctx.isSubprojects) {
      newlines = Whitespace.getBlankSpaceToLeft(tokens, ctx)
    }
    if (ctx.isBuildscript) {
      whitespace = Whitespace.getWhitespaceToLeft(tokens, ctx)
    }
    if (ctx.isDependencies) {
      dependencyDeclarations += dependencyExtractor.collectDependencies(ctx)
        .getDependencyDeclarations()
    }
  }
}
