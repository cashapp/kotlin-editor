package cash.recipes.plugins

import cash.recipes.plugins.exception.NonNormalizedScriptException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

internal class PluginMutatorTest {
  @Test
  fun `can add plugins to at the top of existing plugin block`() {
    val pluginToAdd = setOf("foo", "bar", "baz")
    val pluginsToRemove = emptySet<String>()

    val buildScript =
      """
        plugins {
          id("baz")
        }
      """.trimIndent()

    // When...
    val pluginMutator = PluginMutator.of(buildScript, pluginToAdd, pluginsToRemove)
    val rewrittenContent = pluginMutator.rewritten()
    val expectedContent =
      """
        plugins {
          id("foo")
          id("bar")
          id("baz")
        }
      """.trimIndent()

    // Then...
    assertThat(rewrittenContent).isEqualTo(expectedContent)
  }

  @Test
  fun `can add plugins to at the top of empty existing plugin block`() {
    val pluginToAdd = setOf("foo", "bar", "baz")
    val pluginsToRemove = emptySet<String>()

    val buildScript =
      """
        plugins {
        }
      """.trimIndent()

    // When...
    val pluginMutator = PluginMutator.of(buildScript, pluginToAdd, pluginsToRemove)
    val rewrittenContent = pluginMutator.rewritten()
    val expectedContent =
      """
        plugins {
          id("foo")
          id("bar")
          id("baz")
        }
      """.trimIndent()

    // Then...
    assertThat(rewrittenContent).isEqualTo(expectedContent)
  }

  @Test
  fun `can add plugins when no existing plugin block is present`() {
    val pluginToAdd = setOf("foo", "bar")
    val pluginsToRemove = emptySet<String>()

    val buildScript =
      """
        extension {
          foo = bar
        }
      """.trimIndent()

    // When...
    val pluginMutator = PluginMutator.of(buildScript, pluginToAdd, pluginsToRemove)
    val rewrittenContent = pluginMutator.rewritten()
    val expectedContent =
      """
        plugins {
          id("foo")
          id("bar")
        }

        extension {
          foo = bar
        }
      """.trimIndent()

    // Then...
    assertThat(rewrittenContent).isEqualTo(expectedContent)
  }

  @Test
  fun `skip adding plugins that are already present`() {
    val pluginToAdd = setOf("cash.server")
    val pluginsToRemove = emptySet<String>()

    val buildScript =
      """
        plugins {
          id("cash.server")
        }
      """.trimIndent()

    // When...
    val pluginMutator = PluginMutator.of(buildScript, pluginToAdd, pluginsToRemove)
    val rewrittenContent = pluginMutator.rewritten()
    val expectedContent =
      """
        plugins {
          id("cash.server")
        }
      """.trimIndent()

    // Then...
    assertThat(rewrittenContent).isEqualTo(expectedContent)
  }

  @Test
  fun `can remove plugins`() {
    val pluginToAdd = emptySet<String>()
    val pluginToRemove = setOf("foo", "bar", "application")

    val buildScript =
      """
        plugins {
          id("foo")
          id("bar")
          application
        }
      """.trimIndent()

    // When...
    val pluginMutator = PluginMutator.of(buildScript, pluginToAdd, pluginToRemove)
    val rewrittenContent = pluginMutator.rewritten()
    val expectedContent =
      """
        plugins {
        }
      """.trimIndent()

    // Then...
    assertThat(rewrittenContent).isEqualTo(expectedContent)
  }

  @Test
  fun `can add and remove of plugins`() {
    val pluginToAdd = setOf("foo", "bar")
    val pluginToRemove = setOf("a-plugin")

    val buildScript =
      """
        plugins {
          id("a-plugin")
          id("b-plugin")
        }

        extension {
          foo = bar
        }
      """.trimIndent()

    // When...
    val pluginMutator = PluginMutator.of(buildScript, pluginToAdd, pluginToRemove)
    val rewrittenContent = pluginMutator.rewritten()
    val expectedContent =
      """
        plugins {
          id("foo")
          id("bar")
          id("b-plugin")
        }

        extension {
          foo = bar
        }
      """.trimIndent()

    // Then...
    assertThat(rewrittenContent).isEqualTo(expectedContent)
  }

  @Test
  fun `throw error when same plugins are in add and remove sets`() {
    val pluginToAdd =  setOf("bar")
    val pluginToRemove = setOf("foo", "bar")

    val buildScript =
      """
        plugins {
          id("foo")
          id("bar")
        }
      """.trimIndent()

    assertThrows(IllegalArgumentException::class.java) {
      PluginMutator.of(buildScript, pluginToAdd, pluginToRemove)
    }
  }

  @Test
  fun `throws error for non-normalized script, with non-BlockId plugins`() {
    val pluginToAdd =  setOf("foo")
    val pluginToRemove = setOf("bar")

    val buildScript =
      """
        plugins {
          kotlin("foo")
          id("bar")
        }
      """.trimIndent()

    assertThrows(NonNormalizedScriptException::class.java) {
      PluginMutator.of(buildScript, pluginToAdd, pluginToRemove)
    }
  }

  @Test
  fun `throws error for non-normalized script, with apply plugins`() {
    val pluginToAdd =  setOf("foo")
    val pluginToRemove = setOf("bar")

    val buildScript =
      """
        apply(plugin = "foo")
      """.trimIndent()

    assertThrows(NonNormalizedScriptException::class.java) {
      PluginMutator.of(buildScript, pluginToAdd, pluginToRemove)
    }
  }
}
