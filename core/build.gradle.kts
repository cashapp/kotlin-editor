plugins {
  id("cash.lib")
}

dependencies {
  api(project((":grammar")))

  api(libs.antlr.runtime)
  api(libs.kotlinStdLib)
}
