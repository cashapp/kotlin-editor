plugins {
  id("cash.lib")
}

dependencies {
  api(libs.kotlinStdLib)
  
  implementation(project(":core"))
  implementation(project(":grammar"))
  implementation(libs.antlr.runtime)
}
