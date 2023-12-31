package org.cirjson.compiler.backend.common

import org.cirjson.compiler.backend.common.SerializationDescriptorUtils.getSyntheticLoadMember
import org.cirjson.compiler.backend.common.SerializationDescriptorUtils.getSyntheticSaveMember
import org.cirjson.compiler.extensions.SerializationDescriptorSerializerPlugin
import org.cirjson.compiler.resolve.*
import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext

abstract class SerializerCodegen(protected val serializerDescriptor: ClassDescriptor, bindingContext: BindingContext,
        metadataPlugin: SerializationDescriptorSerializerPlugin?) :
        AbstractSerialGenerator(bindingContext, serializerDescriptor) {

    val serializableDescriptor: ClassDescriptor = getSerializableClassDescriptorBySerializer(serializerDescriptor)!!

    protected val serialName: String = serializableDescriptor.serialName()

    protected val properties = bindingContext.serializablePropertiesFor(serializableDescriptor, metadataPlugin)

    protected val serializableProperties = properties.serializableProperties

    private fun checkSerializability() {
        check(properties.isExternallySerializable) {
            "Class ${serializableDescriptor.name} have constructor parameters which are not properties and therefore it is not serializable automatically"
        }
    }

    fun generate() {
        val prop = generateSerializableClassPropertyIfNeeded()
        if (prop) generateSerialDesc()
        val save = generateSaveIfNeeded()
        val load = generateLoadIfNeeded()
        generateMembersFromGeneratedSerializer()
        if (!prop && (save || load)) generateSerialDesc()
        if (serializableDescriptor.declaredTypeParameters.isNotEmpty()) {
            findSerializerConstructorForTypeArgumentsSerializers(serializerDescriptor, onlyIfSynthetic = true)?.let {
                generateGenericFieldsAndConstructor(it)
            }
        }
    }

    private fun generateMembersFromGeneratedSerializer() {
        CodegenUtil.getMemberToGenerate(serializerDescriptor, SerialEntityNames.CHILD_SERIALIZERS_GETTER.identifier,
                { true }, { it.isEmpty() })?.let { generateChildSerializersGetter(it) }
        CodegenUtil.getMemberToGenerate(serializerDescriptor,
                SerialEntityNames.TYPE_PARAMS_SERIALIZERS_GETTER.identifier, { true }, { it.isEmpty() })
            ?.takeIf { it.kind != CallableMemberDescriptor.Kind.FAKE_OVERRIDE }
            ?.let { generateTypeParamsSerializersGetter(it) }
    }

    protected abstract fun generateTypeParamsSerializersGetter(function: FunctionDescriptor)

    protected abstract fun generateChildSerializersGetter(function: FunctionDescriptor)

    protected val generatedSerialDescPropertyDescriptor =
            getPropertyToGenerate(serializerDescriptor, SerialEntityNames.SERIAL_DESC_FIELD,
                    serializerDescriptor::checkSerializableClassPropertyResult)

    protected val anySerialDescProperty = getProperty(serializerDescriptor, SerialEntityNames.SERIAL_DESC_FIELD,
            serializerDescriptor::checkSerializableClassPropertyResult) { true }

    var localSerializersFieldsDescriptors: List<Pair<PropertyDescriptor, IrProperty>> = emptyList()
        protected set

    // Can be false if user specified inheritance from KSerializer explicitly
    protected val isGeneratedSerializer = serializerDescriptor.typeConstructor.supertypes.any(::isGeneratedKSerializer)

    protected fun findLocalSerializersFieldDescriptors(): List<PropertyDescriptor> {
        val count = serializableDescriptor.declaredTypeParameters.size
        if (count == 0) return emptyList()
        val propNames = (0 until count).map { "${SerialEntityNames.typeArgPrefix}$it" }
        return propNames.mapNotNull { name ->
            getPropertyToGenerate(serializerDescriptor, name) { isKSerializer(it.returnType) }
        }
    }

    protected abstract fun generateSerialDesc()

    protected abstract fun generateGenericFieldsAndConstructor(typedConstructorDescriptor: ClassConstructorDescriptor)

    protected abstract fun generateSerializableClassProperty(property: PropertyDescriptor)

    protected abstract fun generateSave(function: FunctionDescriptor)

    protected abstract fun generateLoad(function: FunctionDescriptor)

    private fun generateSerializableClassPropertyIfNeeded(): Boolean {
        val property = generatedSerialDescPropertyDescriptor ?: return false
        checkSerializability()
        generateSerializableClassProperty(property)
        return true
    }

    private fun generateSaveIfNeeded(): Boolean {
        val function = getSyntheticSaveMember(serializerDescriptor) ?: return false
        checkSerializability()
        generateSave(function)
        return true
    }

    private fun generateLoadIfNeeded(): Boolean {
        val function = getSyntheticLoadMember(serializerDescriptor) ?: return false
        checkSerializability()
        generateLoad(function)
        return true
    }

    private fun getPropertyToGenerate(classDescriptor: ClassDescriptor, name: String,
            isReturnTypeOk: (PropertyDescriptor) -> Boolean): PropertyDescriptor? =
            getProperty(classDescriptor, name, isReturnTypeOk) { kind ->
                kind == CallableMemberDescriptor.Kind.SYNTHESIZED || kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE
            }

    private fun getProperty(classDescriptor: ClassDescriptor, name: String,
            isReturnTypeOk: (PropertyDescriptor) -> Boolean,
            isKindOk: (CallableMemberDescriptor.Kind) -> Boolean): PropertyDescriptor? =
            classDescriptor.unsubstitutedMemberScope.getContributedVariables(Name.identifier(name),
                    NoLookupLocation.FROM_BACKEND).singleOrNull { property ->
                isKindOk(property.kind) && property.returnType != null && isReturnTypeOk(property)
            }

}