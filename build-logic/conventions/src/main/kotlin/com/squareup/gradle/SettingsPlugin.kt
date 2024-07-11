package com.squareup.gradle

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.kotlin.dsl.develocity
import java.util.Properties

@Suppress("UnstableApiUsage")
public abstract class SettingsPlugin : Plugin<Settings> {

  override fun apply(target: Settings): Unit = target.run {
    pluginManager.apply("com.gradle.develocity")

    val shouldPublish = shouldPublishBuildScans()

    develocity {
      buildScan {
        it.termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
        it.termsOfUseAgree.set("yes")
        it.publishing.onlyIf { shouldPublish }
      }
    }
  }

  private fun Settings.shouldPublishBuildScans(): Boolean {
    val localProperties = Properties()
    var publishBuildScans = false

    val localPropertiesFile = layout.settingsDirectory.file("local.properties").asFile
    if (localPropertiesFile.exists()) {
      localPropertiesFile.inputStream().use {
        localProperties.load(it)
      }

      publishBuildScans = localProperties.getProperty("kotlin.editor.build.scans.enable")
        ?.toBoolean()
        ?: false
    }

    return publishBuildScans
  }
}
