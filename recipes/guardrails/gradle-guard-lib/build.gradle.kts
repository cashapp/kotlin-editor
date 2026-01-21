plugins {
  id("cash.lib")
}

kotlinEditor {
  group("app.cash.gradle-guard")
  version(providers.gradleProperty("cashapp.gradle-guard-version").orElse("0.2.1-SNAPSHOT").get())
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
  implementation(libs.kotlinxCoroutinesCore)
}
