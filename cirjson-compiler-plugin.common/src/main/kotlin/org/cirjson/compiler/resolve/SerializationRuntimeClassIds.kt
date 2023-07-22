package org.cirjson.compiler.resolve

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

object SerializationRuntimeClassIds {

    val descriptorClassId = ClassId(SerializationPackages.descriptorsPackageFqName,
            Name.identifier(SerialEntityNames.SERIAL_DESCRIPTOR_CLASS))

    val compositeEncoderClassId = ClassId(SerializationPackages.encodingPackageFqName,
            Name.identifier(SerialEntityNames.STRUCTURE_ENCODER_CLASS))

}