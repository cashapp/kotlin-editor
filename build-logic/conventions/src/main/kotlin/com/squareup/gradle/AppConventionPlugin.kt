package com.squareup.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * ```
 * plugins {
 *   id("cash.app")
 * }
 * ```
 */
public abstract class AppConventionPlugin : Plugin<Project> {
  override fun apply(target: Project): Unit = target.run {
    pluginManager.run {
      apply("application")
      apply("com.gradleup.shadow")
      apply(LibraryConventionPlugin::class.java)
    }
  }
}
