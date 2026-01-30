// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
// Taken directly fom https://github.com/autonomousapps/dependency-analysis-gradle-plugin/blob/d42dd3122e090491ce705d153588956ffe92b600/src/main/kotlin/com/autonomousapps/internal/utils/comparators.kt#L5
package cash.recipes.lint.buildscripts.utils.comparable

internal class LexicographicIterableComparator<T : Comparable<T>> : Comparator<Iterable<T>> {
  override fun compare(left: Iterable<T>?, right: Iterable<T>?): Int {
    if (left === right) return 0
    if (left == null || right == null) return if (left == null) -1 else 1

    val leftIterator = left.iterator()
    val rightIterator = right.iterator()

    while (leftIterator.hasNext() && rightIterator.hasNext()) {
      val compareResult = leftIterator.next().compareTo(rightIterator.next())
      if (compareResult != 0) {
        return compareResult
      }
    }

    if (leftIterator.hasNext()) return 1
    if (rightIterator.hasNext()) return -1

    return 0
  }
}
