package org.cirjson.compiler.diagnostic

import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.KotlinCompilerVersion

data class RuntimeVersions(val implementationVersion: ApiVersion?, val requireKotlinVersion: ApiVersion?) {

    fun currentCompilerMatchRequired(): Boolean {
        val current = requireNotNull(KotlinCompilerVersion.getVersion()?.let(ApiVersion.Companion::parse))
        return requireKotlinVersion == null || requireKotlinVersion <= current
    }

    fun implementationVersionMatchSupported(): Boolean {
        return implementationVersion != null && implementationVersion >= MINIMAL_SUPPORTED_VERSION
    }

    companion object {

        val MINIMAL_SUPPORTED_VERSION = ApiVersion.parse("1.0-M1-SNAPSHOT")!!

        val MINIMAL_VERSION_FOR_INLINE_CLASSES = ApiVersion.parse("1.1-M1-SNAPSHOT")!!

    }

}

