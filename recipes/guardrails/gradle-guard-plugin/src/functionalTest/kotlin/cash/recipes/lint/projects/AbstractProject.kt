package cash.recipes.lint.projects

import com.autonomousapps.kit.AbstractGradleProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.GradleProject.DslKind
import com.autonomousapps.kit.gradle.Plugin

internal abstract class AbstractProject(private val name: String) : AbstractGradleProject() {

  companion object {
    private const val ID = "app.cash.gradle-guard"
    private val GRADLE_GUARD_PLUGIN = Plugin(ID, PLUGIN_UNDER_TEST_VERSION)
  }

  override fun newGradleProjectBuilder(): GradleProject.Builder {
    return super.newGradleProjectBuilder(DslKind.KOTLIN)
      .withRootProject {
        withSettingsScript {
          rootProjectName = name
        }
        withBuildScript {
          plugins(GRADLE_GUARD_PLUGIN)
        }
      }
  }
}
