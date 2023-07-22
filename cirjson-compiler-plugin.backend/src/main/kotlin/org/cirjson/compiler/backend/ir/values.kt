package org.cirjson.compiler.backend.ir

import org.cirjson.compiler.resolve.SerializationAnnotations
import org.jetbrains.kotlin.backend.jvm.ir.getStringConstArgument
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.*

internal val IrClass.isInternalSerializable: Boolean
    get() {
        if (kind != ClassKind.CLASS) return false
        return hasSerializableOrMetaAnnotationWithoutArgs()
    }

internal val IrClass.isAbstractOrSealedSerializableClass: Boolean get() = isInternalSerializable && (modality == Modality.ABSTRACT || modality == Modality.SEALED)

internal val IrClass.isStaticSerializable: Boolean get() = this.typeParameters.isEmpty()

internal val IrClass.hasCompanionObjectAsSerializer: Boolean
    get() = isInternallySerializableObject || companionObject()?.serializerForClass == this.symbol

internal val IrClass.isInternallySerializableObject: Boolean
    get() = kind == ClassKind.OBJECT && hasSerializableOrMetaAnnotationWithoutArgs()

internal val IrClass.isSealedSerializableInterface: Boolean
    get() = kind == ClassKind.INTERFACE && modality == Modality.SEALED && hasSerializableOrMetaAnnotation()

internal val IrClass.isSerialInfoAnnotation: Boolean
    get() = annotations.hasAnnotation(SerializationAnnotations.serialInfoFqName) || annotations.hasAnnotation(
            SerializationAnnotations.inheritableSerialInfoFqName) || annotations.hasAnnotation(
            SerializationAnnotations.metaSerializableAnnotationFqName)

internal val IrClass.isInheritableSerialInfoAnnotation: Boolean
    get() = annotations.hasAnnotation(SerializationAnnotations.inheritableSerialInfoFqName)

internal val IrClass.shouldHaveGeneratedMethodsInCompanion: Boolean
    get() = this.isSerializableObject || this.isSerializableEnum() || (this.kind == ClassKind.CLASS && hasSerializableOrMetaAnnotation()) || this.isSealedSerializableInterface

internal val IrType.genericIndex: Int?
    get() = (this.classifierOrNull as? IrTypeParameterSymbol)?.owner?.index

internal val IrConstructorCall.constructedClass
    get() = this.symbol.owner.constructedClass

internal val List<IrConstructorCall>.hasAnySerialAnnotation: Boolean
    get() = serialNameValue != null || any { it.constructedClass.isSerialInfoAnnotation }

internal val List<IrConstructorCall>.serialNameValue: String?
    get() = findAnnotation(SerializationAnnotations.serialNameAnnotationFqName)?.getStringConstArgument(
            0) // @SerialName("foo")

val IrClass.primaryConstructorOrFail
    get() = primaryConstructor ?: error("$this is expected to have a primary constructor")

internal val IrType.overriddenSerializer: IrClassSymbol?
    get() {
        annotations.serializableWith()?.let { return it }
        val desc = this.classOrNull ?: return null
        desc.owner.serializableWith?.let { return it }
        return null
    }

internal val IrClass.serializableWith: IrClassSymbol?
    get() = annotations.serializableWith()

internal val IrClass.serializerForClass: IrClassSymbol?
    get() = (annotations.findAnnotation(SerializationAnnotations.serializerAnnotationFqName)
        ?.getValueArgument(0) as? IrClassReference)?.symbol as? IrClassSymbol
