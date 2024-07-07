package com.squareup.gradle.utils

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension

internal class DependencyCatalog(project: Project) {

  val catalog: VersionCatalog = project
    .extensions
    .getByType(VersionCatalogsExtension::class.java)
    .named("libs")
}
