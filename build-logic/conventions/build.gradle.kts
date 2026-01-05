import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("java-gradle-plugin")
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.dependencyAnalysis)
}

gradlePlugin {
  plugins {
    create("lib") {
      id = "cash.lib"
      implementationClass = "com.squareup.gradle.LibraryConventionPlugin"
    }
    create("grammar") {
      id = "cash.grammar"
      implementationClass = "com.squareup.gradle.GrammarConventionPlugin"
    }
    create("settings") {
      id = "cash.settings"
      implementationClass = "com.squareup.gradle.SettingsPlugin"
    }
  }
}

kotlin {
  explicitApi()
}

dependencies {
  api(libs.kotlinStdLib)

  implementation(libs.dependencyAnalysisPlugin)
  implementation(libs.develocityPlugin)
  implementation(libs.kotlinGradlePlugin)
  implementation(libs.kotlinGradlePluginApi)
  implementation(libs.mavenPublish)
}

val javaTarget = JavaLanguageVersion.of(libs.versions.java.get())

java {
  toolchain {
    languageVersion = javaTarget
  }
}

tasks.withType<JavaCompile> {
  options.release.set(javaTarget.asInt())
}

tasks.withType<KotlinCompile> {
  compilerOptions {
    jvmTarget = JvmTarget.fromTarget(javaTarget.toString())
  }
}

tasks.named<Test>("test") {
  useJUnitPlatform()
}
