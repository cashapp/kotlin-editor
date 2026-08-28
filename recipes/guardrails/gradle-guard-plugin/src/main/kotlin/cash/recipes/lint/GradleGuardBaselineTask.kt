package cash.recipes.lint

import cash.recipes.lint.internal.GradleGuardTask
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.CacheableTask
import org.gradle.process.ExecOperations
import javax.inject.Inject

@CacheableTask
public abstract class GradleGuardBaselineTask  @Inject constructor(
  execOps: ExecOperations,
) : GradleGuardTask(execOps) {

  init {
    group = JavaBasePlugin.VERIFICATION_GROUP
    description = "Runs Gradle Guard in 'baseline' mode"
  }
}