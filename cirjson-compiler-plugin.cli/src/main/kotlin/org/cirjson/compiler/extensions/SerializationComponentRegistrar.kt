package org.cirjson.compiler.extensions

import org.cirjson.compiler.extensions.SerializationConfigurationKeys.DISABLE_INTRINSIC
import org.cirjson.serialization.compiler.fir.FirSerializationExtensionRegistrar
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.js.translate.extensions.JsSyntheticTranslateExtension
import org.jetbrains.kotlin.library.metadata.KlibMetadataSerializerProtocol
import org.jetbrains.kotlin.metadata.SerializationPluginMetadataExtensions
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.serialization.DescriptorSerializerPlugin
import org.jetbrains.kotlin.serialization.js.JsSerializerProtocol

@OptIn(ExperimentalCompilerApi::class)
class SerializationComponentRegistrar : CompilerPluginRegistrar() {

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        Companion.registerExtensions(this, loadDisableIntrinsic(configuration))
    }

    private fun loadDisableIntrinsic(configuration: CompilerConfiguration) = if (configuration.get(
                    DISABLE_INTRINSIC) == true) SerializationIntrinsicsState.DISABLED else SerializationIntrinsicsState.NORMAL

    override val supportsK2: Boolean
        get() = true

    companion object {

        fun registerExtensions(extensionStorage: ExtensionStorage,
                intrinsicsState: SerializationIntrinsicsState = SerializationIntrinsicsState.NORMAL) =
            with(extensionStorage) {
                // This method is never called in the IDE, therefore this extension is not available there.
                // Since IDE does not perform any serialization of descriptors, metadata written to the 'serializationDescriptorSerializer'
                // is never deleted, effectively causing memory leaks.
                // So we create SerializationDescriptorSerializerPlugin only outside of IDE.
                val serializationDescriptorSerializer = SerializationDescriptorSerializerPlugin()
                DescriptorSerializerPlugin.registerExtension(serializationDescriptorSerializer)
                registerProtoExtensions()

                SyntheticResolveExtension.registerExtension(
                        SerializationResolveExtension(serializationDescriptorSerializer))

                ExpressionCodegenExtension.registerExtension(
                        SerializationCodegenExtension(serializationDescriptorSerializer))
                JsSyntheticTranslateExtension.registerExtension(
                        SerializationJsExtension(serializationDescriptorSerializer))
                IrGenerationExtension.registerExtension(
                        SerializationLoweringExtension(serializationDescriptorSerializer, intrinsicsState))

                StorageComponentContainerContributor.registerExtension(
                        SerializationPluginComponentContainerContributor())

                FirExtensionRegistrarAdapter.registerExtension(FirSerializationExtensionRegistrar())
            }

        private fun registerProtoExtensions() {
            SerializationPluginMetadataExtensions.registerAllExtensions(JvmProtoBufUtil.EXTENSION_REGISTRY)
            SerializationPluginMetadataExtensions.registerAllExtensions(JsSerializerProtocol.extensionRegistry)
            SerializationPluginMetadataExtensions.registerAllExtensions(
                    KlibMetadataSerializerProtocol.extensionRegistry)
        }

    }

}