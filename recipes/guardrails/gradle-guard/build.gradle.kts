plugins {
  id("cash.app")
}

version = "0.1-SNAPSHOT"
group = "app.cash.gradle-guard"

application {
  mainClass = "cash.recipes.lint.cli.Main"
}

dependencies {
  api(libs.kotlinStdLib)

  implementation(project(":recipes:guardrails:gradle-guard-lib"))
  implementation(libs.clikt)
  implementation(libs.cliktCore)
}
