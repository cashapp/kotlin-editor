package cash.grammar.kotlindsl.utils

import cash.grammar.kotlindsl.parse.Parser
import cash.grammar.kotlindsl.utils.test.TestErrorListener
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class WhitespaceTest {

  @Test fun `can extract newlines`() {
    val buildScript =
      """
        plugins {
          id("kotlin")
        }
        
        subprojects {
          buildscript {
            repositories {}
          }
        }
      """.trimIndent()

    val scriptListener = Parser(
      file = buildScript,
      errorListener = TestErrorListener {
        throw RuntimeException("Syntax error: ${it?.message}", it)
      },
      listenerFactory = { input, tokens, _ -> TestListener(input, tokens) }
    ).listener()

    // There are two newlines preceding the 'subprojects' block
    assertThat(scriptListener.newlines).isNotNull()
    assertThat(scriptListener.newlines!!.size).isEqualTo(2)
    assertThat(scriptListener.newlines!!)
      .extracting({ it.text })
      .allSatisfy { assertThat(it).isEqualTo(tuple("\n")) }
  }

  @Test fun `can extract whitespace`() {
    val buildScript =
      """
        plugins {
          id("kotlin")
        }
        
        subprojects {
          buildscript {
            repositories {}
          }
        }
      """.trimIndent()

    val scriptListener = Parser(
      file = buildScript,
      errorListener = TestErrorListener {
        throw RuntimeException("Syntax error: ${it?.message}", it)
      },
      listenerFactory = { input, tokens, _ -> TestListener(input, tokens) }
    ).listener()

    // There are two spaces preceding the 'buildscript' block (on the same line)
    assertThat(scriptListener.whitespace).isNotNull()
    assertThat(scriptListener.whitespace!!.size).isEqualTo(2)
    assertThat(scriptListener.whitespace!!)
      .extracting({ it.text })
      .allSatisfy { assertThat(it).isEqualTo(tuple(" ")) }
  }

  @Test fun `gets trailing newlines for buildscript`() {
    val buildScript =
      """
        plugins {
          id("kotlin")
        }
        
        subprojects {
          buildscript {
            repositories {}
          }
        }
        
      """.trimIndent()

    val scriptListener = Parser(
      file = buildScript,
      errorListener = TestErrorListener {
        throw RuntimeException("Syntax error: ${it?.message}", it)
      },
      listenerFactory = { input, tokens, _ -> TestListener(input, tokens) }
    ).listener()

    assertThat(scriptListener.trailingBuildscriptNewlines).isEqualTo(1)
  }

  @Test fun `gets trailing newlines for kotlin file`() {
    val file =
      """
        class Foo {
        }
        
      """.trimIndent()

    val scriptListener = Parser(
      file = file.byteInputStream(),
      errorListener = TestErrorListener {
        throw RuntimeException("Syntax error: ${it?.message}", it)
      },
      startRule = { parser -> parser.kotlinFile() },
      listenerFactory = { input, tokens, _ -> TestListener(input, tokens) }
    ).listener()

    assertThat(scriptListener.trailingKotlinFileNewlines).isEqualTo(1)
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("indentationCases")
  fun `can discover indentation`(testCase: TestCase) {
    val parser = Parser(
      file = testCase.sourceText,
      errorListener = TestErrorListener {
        throw RuntimeException("Syntax error: ${it?.message}", it)
      },
      listenerFactory = { input, tokens, _ ->
        TestListener(
          input = input,
          tokens = tokens,
          defaultIndent = testCase.defaultIndent,
        )
      }
    ).listener()

    assertThat(parser.indent).isEqualTo(testCase.expectedIndent)
  }

  private companion object {
    @JvmStatic fun indentationCases() = listOf(
      TestCase(
        displayName = "two spaces",
        sourceText = """
          plugins {
            id("kotlin")
          }
        """.trimIndent(),
        expectedIndent = "  ",
      ),
      TestCase(
        displayName = "four spaces",
        sourceText = """
          plugins {
              id("kotlin")
          }
        """.trimIndent(),
        expectedIndent = "    ",
      ),
      TestCase(
        displayName = "tab",
        sourceText = "plugins {\n\tid(\"kotlin\")\n}",
        expectedIndent = "\t",
      ),
      TestCase(
        displayName = "mixed spaces and tab",
        sourceText = "plugins {\n\t  id(\"kotlin\")\n}",
        expectedIndent = "\t  ",
      ),
      TestCase(
        displayName = "defaults to two spaces",
        sourceText = """
          package com.example
          
          class Foo
        """.trimIndent(),
        expectedIndent = "  ",
      ),
      TestCase(
        displayName = "ignores empty lines",
        // the line between `package...` and `class...` contains a single space -- don't count this
        sourceText = "package com.example\n \nclass Foo",
        expectedIndent = "  ",
      ),
      TestCase(
        displayName = "can change default to tab",
        sourceText = """
          package com.example

          class Foo
        """.trimIndent(),
        defaultIndent = "\t",
        expectedIndent = "\t",
      ),
        TestCase(
            displayName = "maintains indentation when comments are present",
            sourceText = """
          /*
           * Copyright (C) 2018 Square, Inc.
           * SPDX-License-Identifier: Apache 2.0
           */
          package com.example
          
          class Foo
        """.trimIndent(),
            expectedIndent = "  ",
        ),
    )
  }

  internal class TestCase(
    val displayName: String,
    val sourceText: String,
    val defaultIndent: String = "  ",
    val expectedIndent: String,
  ) {
    override fun toString(): String = displayName
  }
}
