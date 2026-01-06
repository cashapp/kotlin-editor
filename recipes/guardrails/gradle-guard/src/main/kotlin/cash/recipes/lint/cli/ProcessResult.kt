package cash.recipes.lint.cli

import kotlin.system.exitProcess

// TODO(tsr): consider adding more error codes and making public if we think other tools will want to use these return
//  values.
internal enum class ProcessResult(val value: Int) {
  SUCCESS(0),
  FAILURE(1),
  ;

  companion object {
    fun Result<ProcessResult>.handleResult(): Nothing {
      if (isSuccess) {
        exitProcess(getOrThrow().value)
      } else {
        exceptionOrNull()?.printStackTrace()
        exitProcess(FAILURE.value)
      }
    }
  }
}
