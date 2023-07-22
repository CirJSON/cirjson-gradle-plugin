package org.cirjson.compiler.backend.common

import org.cirjson.compiler.resolve.*
import org.cirjson.compiler.resolve.SerialEntityNames.SERIALIZER_PROVIDER_NAME
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.resolve.BindingContext

abstract class SerializableCompanionCodegen(protected val companionDescriptor: ClassDescriptor,
        bindingContext: BindingContext?) : AbstractSerialGenerator(bindingContext, companionDescriptor) {

    protected val serializableDescriptor: ClassDescriptor =
            getSerializableClassDescriptorByCompanion(companionDescriptor)!!

    open fun getSerializerGetterDescriptor(): FunctionDescriptor {
        return findSerializerGetterOnCompanion(serializableDescriptor) ?: throw IllegalStateException(
                "Can't find synthesized 'Companion.serializer()' function to generate, " + "probably clash with user-defined function has occurred")
    }

    fun generate() {
        val serializerGetterDescriptor = getSerializerGetterDescriptor()

        if (serializableDescriptor.isSerializableObject || serializableDescriptor.isAbstractOrSealedSerializableClass() || serializableDescriptor.isSerializableEnum()) {
            generateLazySerializerGetter(serializerGetterDescriptor)
        } else {
            generateSerializerGetter(serializerGetterDescriptor)
        }
    }

    protected abstract fun generateSerializerGetter(methodDescriptor: FunctionDescriptor)

    protected open fun generateLazySerializerGetter(methodDescriptor: FunctionDescriptor) {
        generateSerializerGetter(methodDescriptor)
    }

    companion object {

        fun findSerializerGetterOnCompanion(serializableDescriptor: ClassDescriptor): FunctionDescriptor? {
            val companionObjectDesc =
                    if (serializableDescriptor.isSerializableObject) serializableDescriptor else serializableDescriptor.companionObjectDescriptor
            return companionObjectDesc?.unsubstitutedMemberScope?.getContributedFunctions(SERIALIZER_PROVIDER_NAME,
                    NoLookupLocation.FROM_BACKEND)?.firstOrNull {
                it.valueParameters.size == serializableDescriptor.declaredTypeParameters.size && it.kind == CallableMemberDescriptor.Kind.SYNTHESIZED && it.valueParameters.all { p ->
                    isKSerializer(p.type)
                } && it.returnType != null && isKSerializer(it.returnType)
            }
        }
    }

}