package org.cirjson.compiler.resolve

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.hasBackingField
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor

class SerializableProperties(private val serializableClass: ClassDescriptor, val bindingContext: BindingContext) :
        ISerializableProperties<SerializableProperty> {

    private val primaryConstructorParameters: List<ValueParameterDescriptor> =
            serializableClass.unsubstitutedPrimaryConstructor?.valueParameters ?: emptyList()

    override val serializableProperties: List<SerializableProperty>

    override val isExternallySerializable: Boolean

    private val primaryConstructorProperties: Map<PropertyDescriptor, Boolean>

    init {
        val descriptorsSequence =
                serializableClass.unsubstitutedMemberScope.getContributedDescriptors(DescriptorKindFilter.VARIABLES)
                    .asSequence() // call to any BindingContext.get should be only AFTER MemberScope.getContributedDescriptors
        primaryConstructorProperties = primaryConstructorParameters.asSequence()
            .map { parameter -> bindingContext[BindingContext.VALUE_PARAMETER_AS_PROPERTY, parameter] to parameter.declaresDefaultValue() }
            .mapNotNull { (a, b) -> if (a == null) null else a to b }.toMap()

        fun isPropSerializable(it: PropertyDescriptor) =
                if (serializableClass.isInternalSerializable) !it.annotations.serialTransient
                else !DescriptorVisibilities.isPrivate(
                        it.visibility) && ((it.isVar && !it.annotations.serialTransient) || primaryConstructorProperties.contains(
                        it))

        serializableProperties = descriptorsSequence.filterIsInstance<PropertyDescriptor>()
            .filter { it.kind == CallableMemberDescriptor.Kind.DECLARATION }.filter(::isPropSerializable).map { prop ->
                val declaresDefaultValue = prop.declaresDefaultValue()
                SerializableProperty(prop, primaryConstructorProperties[prop] ?: false, prop.hasBackingField(
                        bindingContext) || (prop is DeserializedPropertyDescriptor && prop.backingField != null) // workaround for TODO in .hasBackingField
                        // workaround for overridden getter (val) and getter+setter (var) - in this case hasBackingField returning false
                        // but initializer presents only for property with backing field
                        || declaresDefaultValue, declaresDefaultValue)
            }.filterNot { it.transient }.partition { primaryConstructorProperties.contains(it.descriptor) }.run {
                val supers = serializableClass.getSuperClassNotAny()
                if (supers == null || !supers.isInternalSerializable) first + second
                else SerializableProperties(supers, bindingContext).serializableProperties + first + second
            }.let { restoreCorrectOrderFromClassProtoExtension(serializableClass, it) }

        isExternallySerializable =
                serializableClass.isInternallySerializableEnum() || primaryConstructorParameters.size == primaryConstructorProperties.size

    }

    override val serializableConstructorProperties: List<SerializableProperty> =
            serializableProperties.asSequence().filter { primaryConstructorProperties.contains(it.descriptor) }.toList()

    override val serializableStandaloneProperties: List<SerializableProperty> =
            serializableProperties.minus(serializableConstructorProperties)

    val size = serializableProperties.size

    operator fun get(index: Int) = serializableProperties[index]

    operator fun iterator() = serializableProperties.iterator()

    val primaryConstructorWithDefaults =
            serializableClass.unsubstitutedPrimaryConstructor?.original?.valueParameters?.any { it.declaresDefaultValue() }
                    ?: false

}