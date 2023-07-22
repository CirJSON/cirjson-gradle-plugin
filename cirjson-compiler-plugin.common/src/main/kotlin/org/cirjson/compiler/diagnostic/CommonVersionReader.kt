package org.cirjson.compiler.diagnostic

import com.intellij.openapi.util.io.JarUtil
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import java.io.File
import java.util.jar.Attributes

object CommonVersionReader {

    private val REQUIRE_KOTLIN_VERSION = Attributes.Name("Require-Kotlin-Version")

    private const val CLASS_SUFFIX = "!/kotlinx/serialization/KSerializer.class"

    fun computeRuntimeVersions(sourceElement: SourceElement?): RuntimeVersions? {
        val location = (sourceElement as? KotlinJvmBinarySourceElement)?.binaryClass?.location ?: return null
        val jarFile = location.removeSuffix(CLASS_SUFFIX)
        if (!jarFile.endsWith(".jar")) return null
        val file = File(jarFile)
        if (!file.exists()) return null
        return getVersionsFromManifest(file)
    }

    fun getVersionsFromManifest(runtimeLibraryPath: File): RuntimeVersions {
        val version = JarUtil.getJarAttribute(runtimeLibraryPath, Attributes.Name.IMPLEMENTATION_VERSION)
            ?.let(ApiVersion.Companion::parse)
        val kotlinVersion =
                JarUtil.getJarAttribute(runtimeLibraryPath, REQUIRE_KOTLIN_VERSION)?.let(ApiVersion.Companion::parse)
        return RuntimeVersions(version, kotlinVersion)
    }

    fun canSupportInlineClasses(currentVersion: RuntimeVersions?): Boolean {
        if (currentVersion == null) return true
        val implVersion = currentVersion.implementationVersion ?: return false
        return implVersion >= RuntimeVersions.MINIMAL_VERSION_FOR_INLINE_CLASSES
    }

}