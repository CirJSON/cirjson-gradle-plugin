package org.cirjson.compiler.extensions

import org.cirjson.compiler.backend.ir.SerializationJvmIrIntrinsicSupport
import org.cirjson.compiler.resolve.SerializationPackages
import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrIntrinsicExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

open class SerializationLoweringExtension @JvmOverloads constructor(
        private val metadataPlugin: SerializationDescriptorSerializerPlugin? = null) : IrGenerationExtension {

    private var intrinsicsState = SerializationIntrinsicsState.NORMAL

    constructor(metadataPlugin: SerializationDescriptorSerializerPlugin,
            intrinsicsState: SerializationIntrinsicsState) : this(metadataPlugin) {
        this.intrinsicsState = intrinsicsState
    }

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val pass1 = SerializerClassPreLowering(pluginContext)
        val pass2 = SerializerClassLowering(pluginContext, metadataPlugin, moduleFragment)
        moduleFragment.files.forEach(pass1::runOnFileInOrder)
        moduleFragment.files.forEach(pass2::runOnFileInOrder)
    }

    override fun getPlatformIntrinsicExtension(backendContext: BackendContext): IrIntrinsicExtension? {
        val ctx = backendContext as? JvmBackendContext ?: return null
        if (!canEnableIntrinsics(ctx)) return null
        return SerializationJvmIrIntrinsicSupport(ctx, requireNotNull(
                ctx.irPluginContext) { "Intrinsics can't be enabled with null irPluginContext, check `canEnableIntrinsics` function for bugs." })
    }

    private fun canEnableIntrinsics(ctx: JvmBackendContext): Boolean {
        return when (intrinsicsState) {
            SerializationIntrinsicsState.FORCE_ENABLED -> true
            SerializationIntrinsicsState.DISABLED -> false
            SerializationIntrinsicsState.NORMAL -> {
                val requiredFunctionsFromRuntime = ctx.irPluginContext?.referenceFunctions(
                        CallableId(SerializationPackages.packageFqName,
                                Name.identifier(SerializationJvmIrIntrinsicSupport.noCompiledSerializerMethodName)))
                    .orEmpty()
                requiredFunctionsFromRuntime.isNotEmpty() && requiredFunctionsFromRuntime.all { it.isBound }
            }
        }
    }

}