plugins {
  id("cash.app")
}

kotlinEditor {
  group("app.cash.gradle-guard")
  version(providers.gradleProperty("cashapp.gradle-guard-version").orElse("0.2.0-SNAPSHOT").get())
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
