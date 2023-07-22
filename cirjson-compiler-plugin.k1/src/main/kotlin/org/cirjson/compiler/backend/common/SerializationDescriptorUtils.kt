package org.cirjson.compiler.backend.common

import org.cirjson.compiler.resolve.*
import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor

object SerializationDescriptorUtils {

    fun getSyntheticLoadMember(serializerDescriptor: ClassDescriptor): FunctionDescriptor? =
            CodegenUtil.getMemberToGenerate(serializerDescriptor, SerialEntityNames.LOAD,
                    serializerDescriptor::checkLoadMethodResult, serializerDescriptor::checkLoadMethodParameters)

    fun getSyntheticSaveMember(serializerDescriptor: ClassDescriptor): FunctionDescriptor? =
            CodegenUtil.getMemberToGenerate(serializerDescriptor, SerialEntityNames.SAVE,
                    serializerDescriptor::checkSaveMethodResult, serializerDescriptor::checkSaveMethodParameters)

}