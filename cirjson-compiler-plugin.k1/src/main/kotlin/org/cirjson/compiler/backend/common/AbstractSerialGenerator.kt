package org.cirjson.compiler.backend.common

import org.cirjson.compiler.resolve.SerializationAnnotations
import org.cirjson.compiler.resolve.isKSerializer
import org.cirjson.compiler.resolve.toClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.firstArgument
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.supertypes

abstract class AbstractSerialGenerator(val bindingContext: BindingContext?, val currentDeclaration: ClassDescriptor) {

    private fun getKClassListFromFileAnnotation(annotationFqName: FqName,
            declarationInFile: DeclarationDescriptor): List<KotlinType> {
        if (bindingContext == null) return emptyList()
        val annotation = AnnotationsUtils.getContainingFileAnnotations(bindingContext, declarationInFile)
            .find { it.fqName == annotationFqName } ?: return emptyList()

        @Suppress("UNCHECKED_CAST") val typeList: List<KClassValue> =
                annotation.firstArgument()?.value as? List<KClassValue> ?: return emptyList()
        return typeList.map { it.getArgumentType(declarationInFile.module) }
    }

    val contextualKClassListInCurrentFile: Set<KotlinType> by lazy {
        getKClassListFromFileAnnotation(SerializationAnnotations.contextualFqName, currentDeclaration).plus(
                getKClassListFromFileAnnotation(SerializationAnnotations.contextualOnFileFqName, currentDeclaration))
            .toSet()
    }

    val additionalSerializersInScopeOfCurrentFile: Map<Pair<ClassDescriptor, Boolean>, ClassDescriptor> by lazy {
        getKClassListFromFileAnnotation(SerializationAnnotations.additionalSerializersFqName,
                currentDeclaration).associateBy({
            val kotlinType = it.supertypes().find(::isKSerializer)?.arguments?.firstOrNull()?.type
            val descriptor = kotlinType.toClassDescriptor ?: throw AssertionError(
                    "Argument for ${SerializationAnnotations.additionalSerializersFqName} does not implement KSerializer or does not provide serializer for concrete type")
            descriptor to kotlinType!!.isMarkedNullable
        }, { it.toClassDescriptor!! })
    }

    protected fun ClassDescriptor.getFuncDesc(funcName: String): Sequence<FunctionDescriptor> =
            unsubstitutedMemberScope.getDescriptorsFiltered { it == Name.identifier(funcName) }.asSequence()
                .filterIsInstance<FunctionDescriptor>()

}