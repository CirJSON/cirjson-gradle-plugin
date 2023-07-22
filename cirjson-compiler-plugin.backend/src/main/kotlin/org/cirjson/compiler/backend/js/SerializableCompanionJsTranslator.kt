package org.cirjson.compiler.backend.js

import org.cirjson.compiler.backend.common.SerializableCompanionCodegen
import org.cirjson.compiler.backend.common.findTypeSerializer
import org.cirjson.compiler.resolve.getSerializableClassDescriptorByCompanion
import org.cirjson.compiler.resolve.shouldHaveGeneratedMethodsInCompanion
import org.cirjson.compiler.resolve.toSimpleType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.js.backend.ast.JsNameRef
import org.jetbrains.kotlin.js.backend.ast.JsReturn
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.declaration.DeclarationBodyVisitor
import org.jetbrains.kotlin.resolve.descriptorUtil.module

class SerializableCompanionJsTranslator(declaration: ClassDescriptor, val translator: DeclarationBodyVisitor,
        val context: TranslationContext) : SerializableCompanionCodegen(declaration, context.bindingContext()) {

    override fun generateSerializerGetter(methodDescriptor: FunctionDescriptor) {
        val f = context.buildFunction(methodDescriptor) { jsFun, context ->
            val serializer =
                requireNotNull(findTypeSerializer(serializableDescriptor.module, serializableDescriptor.toSimpleType()))
            val args = jsFun.parameters.map { JsNameRef(it.name) }
            val stmt = requireNotNull(serializerInstance(context, serializer, serializableDescriptor.module,
                    serializableDescriptor.defaultType, genericGetter = { it, _ ->
                args[it]
            }))
            +JsReturn(stmt)
        }
        translator.addFunction(methodDescriptor, f, null)
    }

    companion object {

        fun translate(descriptor: ClassDescriptor, translator: DeclarationBodyVisitor, context: TranslationContext) {
            val serializableClass = getSerializableClassDescriptorByCompanion(descriptor) ?: return
            if (serializableClass.shouldHaveGeneratedMethodsInCompanion) SerializableCompanionJsTranslator(descriptor,
                    translator, context).generate()
        }

    }

}