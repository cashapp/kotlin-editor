import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `java-gradle-plugin`
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
  }
}

kotlin {
  explicitApi()
}

dependencies {
  api(libs.kotlinStdLib)

  implementation(libs.dependencyAnalysisPlugin)
  implementation(libs.kotlinGradlePlugin)
  implementation(libs.kotlinGradlePluginApi)

  testImplementation(libs.assertj)
  testImplementation(libs.junit.jupiter.api)

  testRuntimeOnly(libs.junit.jupiter.engine)
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
  kotlinOptions {
    jvmTarget = javaTarget.toString()
  }
}

tasks.named<Test>("test") {
  useJUnitPlatform()
}
