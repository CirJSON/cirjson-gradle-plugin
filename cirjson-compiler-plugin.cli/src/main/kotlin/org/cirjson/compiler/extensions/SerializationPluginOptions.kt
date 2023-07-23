package org.cirjson.compiler.extensions

import org.cirjson.compiler.extensions.SerializationConfigurationKeys.DISABLE_INTRINSIC
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.CompilerConfiguration

@OptIn(ExperimentalCompilerApi::class)
class SerializationPluginOptions : CommandLineProcessor {

    override val pluginId = "org.cirjson.plugin.serialization"

    override val pluginOptions = listOf(DISABLE_INTRINSIC_OPTION)

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) =
        when (option) {
            DISABLE_INTRINSIC_OPTION -> configuration.put(DISABLE_INTRINSIC, value == "true")
            else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
        }

    companion object {

        val DISABLE_INTRINSIC_OPTION = CliOption("disableIntrinsic", "true/false",
                "Disable replacement of serializer<T>() call with direct serializer retrieval. Use if you experience errors during inlining.",
                required = false, allowMultipleOccurrences = false)

    }

}