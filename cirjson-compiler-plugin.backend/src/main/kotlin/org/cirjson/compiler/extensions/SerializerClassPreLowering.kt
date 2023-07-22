package org.cirjson.compiler.extensions

import org.cirjson.compiler.backend.ir.IrPreGenerator
import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

internal class SerializerClassPreLowering(baseContext: IrPluginContext) : IrElementTransformerVoid(),
        ClassLoweringPass {

    val context: SerializationPluginContext = SerializationPluginContext(baseContext, null)

    override fun lower(irClass: IrClass) {
        irClass.runPluginSafe {
            IrPreGenerator.generate(irClass, context)
        }
    }

}