package org.cirjson.compiler.backend.jvm

import org.cirjson.compiler.backend.common.SerializableCompanionCodegen
import org.cirjson.compiler.backend.common.findTypeSerializer
import org.cirjson.compiler.resolve.SerialEntityNames.CACHED_SERIALIZER_PROPERTY
import org.cirjson.compiler.resolve.SerializationDependencies.LAZY_PUBLICATION_MODE_NAME
import org.cirjson.compiler.resolve.getKSerializer
import org.cirjson.compiler.resolve.getSerializableClassDescriptorByCompanion
import org.cirjson.compiler.resolve.shouldHaveGeneratedMethodsInCompanion
import org.cirjson.compiler.resolve.toSimpleType
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.org.objectweb.asm.Opcodes

class SerializableCompanionCodegenImpl(private val classCodegen: ImplementationBodyCodegen) :
    SerializableCompanionCodegen(classCodegen.descriptor, classCodegen.bindingContext) {

    override fun generateLazySerializerGetter(methodDescriptor: FunctionDescriptor) {
        val fieldName = "$CACHED_SERIALIZER_PROPERTY\$delegate"

        // Create field for lazy delegate
        classCodegen.v.newField(OtherOrigin(classCodegen.myClass.psiOrParent),
                Opcodes.ACC_PRIVATE or Opcodes.ACC_FINAL or Opcodes.ACC_SYNTHETIC or Opcodes.ACC_STATIC, fieldName,
                kotlinLazyType.descriptor, "L${kotlinLazyType.internalName}<L${kSerializerType.internalName}<*>;>;",
                null)

        // create singleton lambda class
        val lambdaType = createSingletonLambda("serializer\$1", classCodegen,
                companionDescriptor.getKSerializer().defaultType) { lambdaCodegen, expressionCodegen ->
            val serializerDescriptor =
                requireNotNull(findTypeSerializer(serializableDescriptor.module, serializableDescriptor.toSimpleType()))
            stackValueSerializerInstance(expressionCodegen, lambdaCodegen, serializableDescriptor.module,
                    serializableDescriptor.defaultType, serializerDescriptor, this, null)
            areturn(kSerializerType)
        }

        // initialize lazy delegate
        val clInit = classCodegen.createOrGetClInitCodegen()
        with(clInit.v) {
            getstatic(threadSafeModeType.internalName, LAZY_PUBLICATION_MODE_NAME.identifier,
                    threadSafeModeType.descriptor)
            getstatic(lambdaType.internalName, JvmAbi.INSTANCE_FIELD, lambdaType.descriptor)
            checkcast(function0Type)
            invokestatic("kotlin/LazyKt", "lazy",
                    "(${threadSafeModeType.descriptor}${function0Type.descriptor})${kotlinLazyType.descriptor}", false)
            putstatic(classCodegen.className, fieldName, kotlinLazyType.descriptor)
        }

        // create serializer getter
        classCodegen.generateMethod(methodDescriptor) { _, _ ->
            getstatic(classCodegen.className, fieldName, kotlinLazyType.descriptor)
            invokeinterface(kotlinLazyType.internalName, getLazyValueName, "()Ljava/lang/Object;")
            checkcast(kSerializerType)
            areturn(kSerializerType)
        }
    }

    override fun generateSerializerGetter(methodDescriptor: FunctionDescriptor) {
        val serial =
            requireNotNull(findTypeSerializer(serializableDescriptor.module, serializableDescriptor.toSimpleType()))
        classCodegen.generateMethod(methodDescriptor) { _, expressionCodegen ->
            stackValueSerializerInstance(expressionCodegen, classCodegen, serializableDescriptor.module,
                    serializableDescriptor.defaultType, serial, this, null) { it, _ ->
                load(it + 1, kSerializerType)
            }
            areturn(kSerializerType)
        }
    }

    companion object {

        fun generateSerializableExtensions(codegen: ImplementationBodyCodegen) {
            val serializableClass = getSerializableClassDescriptorByCompanion(codegen.descriptor) ?: return
            if (serializableClass.shouldHaveGeneratedMethodsInCompanion) SerializableCompanionCodegenImpl(
                    codegen).generate()
        }

    }

}