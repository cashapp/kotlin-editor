package com.squareup.gradle.extension

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import javax.inject.Inject

public abstract class KotlinEditorExtension @Inject constructor(private val project: Project) {

  private val publishing = project.extensions.getByType(PublishingExtension::class.java)

  private val group: Property<String> = project.objects.property(String::class.java)
  private val version: Property<String> = project.objects.property(String::class.java)

  public fun group(group: String) {
    this.group.set(group)
    this.group.disallowChanges()

    project.group = group

    // This overwrites the group set in BasePlugin which reads from gradle.properties.
    publishing.publications
      .withType(MavenPublication::class.java)
      .named { it == "maven" }
      .configureEach { m -> m.groupId = group }
  }

  public fun version(version: String) {
    this.version.set(version)
    this.version.disallowChanges()

    project.version = version
    // We don't need to configure the publication as with `group` because for some reason `version` is read lazily.
  }

  internal companion object {
    fun of(project: Project): KotlinEditorExtension {
      return project.extensions.create(
        "kotlinEditor",
        KotlinEditorExtension::class.java,
        project,
      )
    }
  }
}
