package org.cirjson.compiler.backend.ir

import org.cirjson.compiler.resolve.ISerializableProperty
import org.cirjson.compiler.resolve.SerializationAnnotations
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.Name

class IrSerializableProperty(val ir: IrProperty, override val isConstructorParameterWithDefault: Boolean,
        hasBackingField: Boolean, declaresDefaultValue: Boolean, val type: IrSimpleType) : ISerializableProperty {

    override val name = ir.annotations.serialNameValue ?: ir.name.asString()

    override val originalDescriptorName: Name = ir.name

    val genericIndex = type.genericIndex

    fun serializableWith(ctx: SerializationBaseContext) =
            ir.annotations.serializableWith() ?: analyzeSpecialSerializers(ctx, ir.annotations)

    override val optional =
            !ir.annotations.hasAnnotation(SerializationAnnotations.requiredAnnotationFqName) && declaresDefaultValue

    override val transient =
            ir.annotations.hasAnnotation(SerializationAnnotations.serialTransientFqName) || !hasBackingField
}

