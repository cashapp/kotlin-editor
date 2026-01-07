package cash.recipes.lint.buildscripts.utils

import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText

internal fun Path.withContent(text: String): Path {
  createParentDirectories()
  writeText(text)
  return this
}
