package cash.grammar.utils

public inline fun <C> C.ifNotEmpty(block: (C) -> Unit) where C : Collection<*> {
  if (isNotEmpty()) {
    block(this)
  }
}
