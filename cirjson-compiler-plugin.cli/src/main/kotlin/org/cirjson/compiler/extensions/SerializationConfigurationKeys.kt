package org.cirjson.compiler.extensions

import org.jetbrains.kotlin.config.CompilerConfigurationKey

object SerializationConfigurationKeys {

    val DISABLE_INTRINSIC: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create("Disable replacement of serializer<T>() call with direct serializer retrieval.")

}