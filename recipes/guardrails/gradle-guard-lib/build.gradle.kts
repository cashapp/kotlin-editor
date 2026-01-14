plugins {
  id("cash.lib")
}

version = "0.1-SNAPSHOT"
group = "app.cash.gradle-guard"

dependencies {
  api(libs.kotlinStdLib)
  
  implementation(project(":core"))
  implementation(project(":grammar"))
  implementation(libs.antlr.runtime)

  implementation(libs.jacksonAnnotations)
  implementation(libs.jacksonCore)
  implementation(libs.jacksonDatabind)
  implementation(libs.jacksonDataformatYaml)
  implementation(libs.jacksonKotlin)
}
