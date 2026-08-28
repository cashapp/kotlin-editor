package cash.recipes.lint

import cash.recipes.lint.projects.ExtensionProject
import cash.recipes.lint.projects.PluginProject
import com.autonomousapps.kit.GradleBuilder.build
import com.autonomousapps.kit.GradleBuilder.buildAndFail
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class PluginTest : AbstractFunctionalTest() {

  @Test
  fun fails() {
    // Given
    val project = PluginProject()
    val gradleProject = project.build()

    // When
    val result = buildAndFail(gradleProject.rootDir, ":gradleGuardCheck", "--ignore-exclude")

    // Then
    assertThat(result.output).contains(project.expectedResult)
  }

  @Test
  fun `creates baseline`() {
    // Given
    val project = PluginProject()
    val gradleProject = project.build()

    // When
    build(gradleProject.rootDir, ":gradleGuardBaseline", "--ignore-exclude", "--output=baseline.yml")

    // Then
    val baseline = gradleProject.rootDir.resolve("baseline.yml")
    assertThat(baseline).exists()
    assertThat(baseline.readText()).isEqualTo(project.expectedBaseline)
  }

  @Test
  fun `fails using extension`() {
    // Given
    val project = ExtensionProject()
    val gradleProject = project.build()

    // When
    val result = buildAndFail(gradleProject.rootDir, ":gradleGuardCheck", "--ignore-exclude")

    // Then
    assertThat(result.output).contains(project.expectedResult)
  }

  @Test
  fun `creates baseline using extension`() {
    // Given
    val project = ExtensionProject()
    val gradleProject = project.build()

    // When
    build(gradleProject.rootDir, ":gradleGuardBaseline", "--ignore-exclude")

    // Then
    val baseline = gradleProject.rootDir.resolve("baseline.yml")
    assertThat(baseline).exists()
    assertThat(baseline.readText()).isEqualTo(project.expectedBaseline)
  }
}
