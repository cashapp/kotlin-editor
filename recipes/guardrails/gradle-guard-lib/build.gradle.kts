plugins {
  id("cash.lib")
}

kotlinEditor {
  group("app.cash.gradle-guard")
  version("0.1-SNAPSHOT")
}

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
