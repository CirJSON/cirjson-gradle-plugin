package org.cirjson.compiler.extensions

import org.cirjson.compiler.resolve.SerializableProperties
import org.cirjson.compiler.resolve.isInternalSerializable
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.SerializationPluginMetadataExtensions
import org.jetbrains.kotlin.metadata.serialization.MutableVersionRequirementTable
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.kotlin.serialization.DescriptorSerializerPlugin
import org.jetbrains.kotlin.serialization.SerializerExtension

class SerializationDescriptorSerializerPlugin : DescriptorSerializerPlugin {

    private val descriptorMetadataMap: MutableMap<ClassDescriptor, SerializableProperties> = hashMapOf()

    private val ClassDescriptor.needSaveProgramOrder: Boolean
        get() = isInternalSerializable && (modality == Modality.OPEN || modality == Modality.ABSTRACT)

    internal fun putIfNeeded(descriptor: ClassDescriptor, properties: SerializableProperties) {
        if (!descriptor.needSaveProgramOrder) return
        descriptorMetadataMap[descriptor] = properties
    }

    override fun afterClass(descriptor: ClassDescriptor, proto: ProtoBuf.Class.Builder,
            versionRequirementTable: MutableVersionRequirementTable, childSerializer: DescriptorSerializer,
            extension: SerializerExtension) {
        fun Name.toIndex() = extension.stringTable.getStringIndex(asString())

        if (!descriptor.needSaveProgramOrder) return

        val propertiesCorrectOrder = (descriptorMetadataMap[descriptor] ?: return).serializableProperties
        proto.setExtension(SerializationPluginMetadataExtensions.propertiesNamesInProgramOrder,
                propertiesCorrectOrder.map { it.descriptor.name.toIndex() })
        descriptorMetadataMap.remove(descriptor)
    }

}