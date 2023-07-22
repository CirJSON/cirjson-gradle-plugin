package org.cirjson.compiler.resolve

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

object SerializersClassIds {

    val kSerializerId = ClassId(SerializationPackages.packageFqName, SerialEntityNames.KSERIALIZER_NAME)

    val enumSerializerId =
            ClassId(SerializationPackages.internalPackageFqName, Name.identifier(SpecialBuiltins.enumSerializer))

    val polymorphicSerializerId =
            ClassId(SerializationPackages.packageFqName, Name.identifier(SpecialBuiltins.polymorphicSerializer))

    val referenceArraySerializerId = ClassId(SerializationPackages.internalPackageFqName,
            Name.identifier(SpecialBuiltins.referenceArraySerializer))

    val objectSerializerId =
            ClassId(SerializationPackages.internalPackageFqName, Name.identifier(SpecialBuiltins.objectSerializer))

    val sealedSerializerId =
            ClassId(SerializationPackages.packageFqName, Name.identifier(SpecialBuiltins.sealedSerializer))

    val contextSerializerId =
            ClassId(SerializationPackages.packageFqName, Name.identifier(SpecialBuiltins.contextSerializer))

    val generatedSerializerId =
            ClassId(SerializationPackages.internalPackageFqName, SerialEntityNames.GENERATED_SERIALIZER_CLASS)

    val setOfSpecialSerializers = setOf(contextSerializerId, polymorphicSerializerId)

}