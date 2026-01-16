plugins {
  id("cash.app")
}

kotlinEditor {
  group("app.cash.gradle-guard")
  version("0.1-SNAPSHOT")
}

application {
  mainClass = "cash.recipes.lint.cli.Main"
}

dependencies {
  api(libs.kotlinStdLib)

  implementation(project(":recipes:guardrails:gradle-guard-lib"))
  implementation(libs.clikt)
  implementation(libs.cliktCore)
}
