package org.cirjson.serialization.gradle

import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class SerializationGradleSubplugin : KotlinCompilerPluginSupportPlugin {

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> =
        kotlinCompilation.target.project.provider { emptyList() }

    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact(SERIALIZATION_GROUP_NAME, SERIALIZATION_ARTIFACT_NAME, PLUGIN_VERSION)

    override fun getPluginArtifactForNative(): SubpluginArtifact =
        SubpluginArtifact(SERIALIZATION_GROUP_NAME, SERIALIZATION_ARTIFACT_UNSHADED_NAME, PLUGIN_VERSION)

    override fun getCompilerPluginId() = "org.cirjson.plugin.serialization"

    companion object {

        const val SERIALIZATION_GROUP_NAME = "org.cirjson.gradle.plugin"

        const val SERIALIZATION_ARTIFACT_NAME = "cirjson-compiler-plugin-embeddable"

        const val SERIALIZATION_ARTIFACT_UNSHADED_NAME = "cirjson-gradle-plugin"

        const val PLUGIN_VERSION = "1.0-SNAPSHOT"

    }

}