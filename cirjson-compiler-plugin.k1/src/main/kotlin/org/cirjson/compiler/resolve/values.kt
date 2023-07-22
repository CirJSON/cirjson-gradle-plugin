package org.cirjson.compiler.resolve

import org.cirjson.compiler.resolve.SerialEntityNames.ANNOTATED_ENUM_SERIALIZER_FACTORY_FUNC_NAME
import org.cirjson.compiler.resolve.SerialEntityNames.ENUM_SERIALIZER_FACTORY_FUNC_NAME
import org.cirjson.compiler.resolve.SerializationAnnotations.inheritableSerialInfoFqName
import org.cirjson.compiler.resolve.SerializationAnnotations.metaSerializableAnnotationFqName
import org.cirjson.compiler.resolve.SerializationAnnotations.serialInfoFqName
import org.cirjson.compiler.resolve.SerializationAnnotations.serializableAnnotationFqName
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.representativeUpperBound

val DeclarationDescriptor.serializableWith: KotlinType?
    get() = annotations.serializableWith(module)

val DeclarationDescriptor.serializerForClass: KotlinType?
    get() = annotations.findAnnotationKotlinTypeValue(SerializationAnnotations.serializerAnnotationFqName, module,
            "forClass")

val ClassDescriptor.isSerialInfoAnnotation: Boolean
    get() = annotations.hasAnnotation(serialInfoFqName) || annotations.hasAnnotation(
            inheritableSerialInfoFqName) || annotations.hasAnnotation(metaSerializableAnnotationFqName)

val ClassDescriptor.isInheritableSerialInfoAnnotation: Boolean
    get() = annotations.hasAnnotation(inheritableSerialInfoFqName)

val Annotations.serialNameValue: String?
    get() = findAnnotationConstantValue(SerializationAnnotations.serialNameAnnotationFqName, "value")

val Annotations.serialNameAnnotation: AnnotationDescriptor?
    get() = findAnnotation(SerializationAnnotations.serialNameAnnotationFqName)

val Annotations.serialRequired: Boolean
    get() = hasAnnotation(SerializationAnnotations.requiredAnnotationFqName)

val Annotations.serialTransient: Boolean
    get() = hasAnnotation(SerializationAnnotations.serialTransientFqName)

// ----------------------------------------

val KotlinType?.toClassDescriptor: ClassDescriptor?
    @JvmName("toClassDescriptor") get() = this?.constructor?.declarationDescriptor?.let { descriptor ->
        when (descriptor) {
            is ClassDescriptor -> descriptor
            is TypeParameterDescriptor -> descriptor.representativeUpperBound.toClassDescriptor
            else -> null
        }
    }

val ClassDescriptor.shouldHaveGeneratedMethodsInCompanion: Boolean
    get() = this.isSerializableObject || this.isSerializableEnum() || (this.kind == ClassKind.CLASS && hasSerializableOrMetaAnnotation) || this.isSealedSerializableInterface

val ClassDescriptor.isSerializableObject: Boolean
    get() = kind == ClassKind.OBJECT && hasSerializableOrMetaAnnotation

val ClassDescriptor.isInternallySerializableObject: Boolean
    get() = kind == ClassKind.OBJECT && hasSerializableOrMetaAnnotationWithoutArgs

val ClassDescriptor.isSealedSerializableInterface: Boolean
    get() = kind == ClassKind.INTERFACE && modality == Modality.SEALED && hasSerializableOrMetaAnnotation

val ClassDescriptor.isAbstractOrSealedOrInterface: Boolean
    get() = kind == ClassKind.INTERFACE || modality == Modality.SEALED || modality == Modality.ABSTRACT

val ClassDescriptor.isInternalSerializable: Boolean //todo normal checking
    get() {
        if (kind != ClassKind.CLASS) return false
        return hasSerializableOrMetaAnnotationWithoutArgs
    }

val ClassDescriptor.shouldHaveGeneratedSerializer: Boolean
    get() = (isInternalSerializable && (modality == Modality.FINAL || modality == Modality.OPEN)) || isEnumWithLegacyGeneratedSerializer()

val ClassDescriptor.useGeneratedEnumSerializer: Boolean
    get() {
        val functions = module.getPackage(SerializationPackages.internalPackageFqName).memberScope.getFunctionNames()
        return !functions.contains(ENUM_SERIALIZER_FACTORY_FUNC_NAME) || !functions.contains(
                ANNOTATED_ENUM_SERIALIZER_FACTORY_FUNC_NAME)
    }

val Annotations.hasAnySerialAnnotation: Boolean
    get() = serialNameValue != null || any { it.annotationClass?.isSerialInfoAnnotation == true }

val ClassDescriptor.hasSerializableOrMetaAnnotation
    get() = hasSerializableAnnotation || hasMetaSerializableAnnotation

private val ClassDescriptor.hasSerializableAnnotation
    get() = annotations.hasSerializableAnnotation

private val Annotations.hasSerializableAnnotation
    get() = hasAnnotation(serializableAnnotationFqName)

val ClassDescriptor.hasMetaSerializableAnnotation: Boolean
    get() = annotations.any { it.isMetaSerializableAnnotation }

val AnnotationDescriptor.isMetaSerializableAnnotation: Boolean
    get() = annotationClass?.annotations?.hasAnnotation(metaSerializableAnnotationFqName) ?: false

val ClassDescriptor.hasSerializableOrMetaAnnotationWithoutArgs: Boolean
    get() = hasSerializableAnnotationWithoutArgs || (!annotations.hasSerializableAnnotation && hasMetaSerializableAnnotation)

private val ClassDescriptor.hasSerializableAnnotationWithoutArgs: Boolean
    get() {
        if (!hasSerializableAnnotation) return false // If provided descriptor is lazy, carefully look at psi in order not to trigger full resolve which may be recursive.
        // Otherwise, this descriptor is deserialized from another module, and it is OK to check value right away.
        val psi = findSerializableAnnotationDeclaration() ?: return (serializableWith == null)
        return psi.valueArguments.isEmpty()
    }

// serializer that was declared for this type
val ClassDescriptor?.classSerializer: ClassDescriptor?
    get() = this?.let { // serializer annotation on class?
        serializableWith?.let { return it.toClassDescriptor } // companion object serializer?
        if (hasCompanionObjectAsSerializer) return companionObjectDescriptor // can infer @Poly?
        polymorphicSerializerIfApplicableAutomatically()?.let { return it } // default serializable?
        if (shouldHaveGeneratedSerializer) { // $serializer nested class
            return this.unsubstitutedMemberScope.getDescriptorsFiltered(
                    nameFilter = { it == SerialEntityNames.SERIALIZER_CLASS_NAME }).filterIsInstance<ClassDescriptor>()
                .singleOrNull()
        }
        return null
    }

val ClassDescriptor.hasCompanionObjectAsSerializer: Boolean
    get() = isInternallySerializableObject || companionObjectDescriptor?.serializerForClass == this.defaultType

val KotlinType.genericIndex: Int?
    get() = (this.constructor.declarationDescriptor as? TypeParameterDescriptor)?.index
