package org.cirjson.compiler.extensions

import org.cirjson.compiler.backend.ir.*
import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.platform.jvm.isJvm

internal class SerializerClassLowering(baseContext: IrPluginContext,
        metadataPlugin: SerializationDescriptorSerializerPlugin?, moduleFragment: IrModuleFragment) :
        IrElementTransformerVoid(), ClassLoweringPass {

    val context: SerializationPluginContext = SerializationPluginContext(baseContext, metadataPlugin)

    // Lazy to avoid creating generator in non-JVM backends
    private val serialInfoJvmGenerator by lazy(LazyThreadSafetyMode.NONE) {
        SerialInfoImplJvmIrGenerator(context, moduleFragment)
    }

    override fun lower(irClass: IrClass) {
        irClass.runPluginSafe {
            SerializableIrGenerator.generate(irClass, context)
            SerializerIrGenerator.generate(irClass, context, context.metadataPlugin)
            SerializableCompanionIrGenerator.generate(irClass, context)

            if (context.platform.isJvm() && irClass.isSerialInfoAnnotation) {
                serialInfoJvmGenerator.generateImplementationFor(irClass)
            }
        }
    }

}
