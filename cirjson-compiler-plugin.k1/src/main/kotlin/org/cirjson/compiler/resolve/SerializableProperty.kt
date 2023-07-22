package org.cirjson.compiler.resolve

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.cirjson.compiler.backend.common.analyzeSpecialSerializers

class SerializableProperty(val descriptor: PropertyDescriptor, override val isConstructorParameterWithDefault: Boolean,
        hasBackingField: Boolean, declaresDefaultValue: Boolean) : ISerializableProperty {

    override val name = descriptor.annotations.serialNameValue ?: descriptor.name.asString()

    override val originalDescriptorName: Name = descriptor.name

    val type = descriptor.type

    val genericIndex = type.genericIndex

    val module = descriptor.module

    val serializableWith =
            descriptor.serializableWith ?: analyzeSpecialSerializers(module, descriptor.annotations)?.defaultType

    override val optional = !descriptor.annotations.serialRequired && declaresDefaultValue

    override val transient = descriptor.annotations.serialTransient || !hasBackingField

}