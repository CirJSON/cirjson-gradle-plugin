package org.cirjson.compiler.resolve

import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object SerialEntityNames {

    const val KSERIALIZER_CLASS = "CircularKSerializer"

    const val SERIAL_DESC_FIELD = "descriptor"

    const val SAVE = "serialize"

    const val LOAD = "deserialize"

    const val SERIALIZER_CLASS = "\$serializer"

    const val CACHED_DESCRIPTOR_FIELD = "\$cachedDescriptor"

    const val CACHED_SERIALIZER_PROPERTY = "\$cachedSerializer"

    const val CACHED_CHILD_SERIALIZERS_PROPERTY = "\$childSerializers"

    // classes
    val KCLASS_NAME_FQ = FqName("kotlin.reflect.KClass")

    val KCLASS_NAME_CLASS_ID = ClassId.topLevel(KCLASS_NAME_FQ)

    val KSERIALIZER_NAME = Name.identifier(KSERIALIZER_CLASS)

    val SERIAL_CTOR_MARKER_NAME = Name.identifier("SerializationConstructorMarker")

    val KSERIALIZER_NAME_FQ = SerializationPackages.packageFqName.child(KSERIALIZER_NAME)

    val KSERIALIZER_CLASS_ID = ClassId.topLevel(KSERIALIZER_NAME_FQ)

    val SERIALIZER_CLASS_NAME = Name.identifier(SERIALIZER_CLASS)

    val IMPL_NAME = Name.identifier("Impl")

    val GENERATED_SERIALIZER_CLASS = Name.identifier("GeneratedCircularSerializer")

    val GENERATED_SERIALIZER_FQ = SerializationPackages.internalPackageFqName.child(GENERATED_SERIALIZER_CLASS)

    val SERIALIZER_FACTORY_INTERFACE_NAME = Name.identifier("CircularSerializerFactory")

    const val ENCODER_CLASS = "CircularEncoder"

    const val STRUCTURE_ENCODER_CLASS = "CircularCompositeEncoder"

    const val DECODER_CLASS = "CircularDecoder"

    const val STRUCTURE_DECODER_CLASS = "CircularCompositeDecoder"

    const val ANNOTATION_MARKER_CLASS = "SerializableWith"

    const val SERIAL_SAVER_CLASS = "CircularSerializationStrategy"

    const val SERIAL_LOADER_CLASS = "CircularDeserializationStrategy"

    const val SERIAL_DESCRIPTOR_CLASS = "CircularSerialDescriptor"

    const val SERIAL_DESCRIPTOR_CLASS_IMPL = "PluginGeneratedSerialDescriptor"

    const val SERIAL_DESCRIPTOR_FOR_ENUM = "CircularEnumDescriptor"

    const val SERIAL_DESCRIPTOR_FOR_INLINE = "InlineClassDescriptor"

    const val PLUGIN_EXCEPTIONS_FILE = "PluginExceptions"

    const val ENUMS_FILE = "Enums"

    //exceptions
    const val SERIAL_EXC = "CircularSerializationException"

    const val MISSING_FIELD_EXC = "MissingFieldException"

    const val UNKNOWN_FIELD_EXC = "UnknownFieldException"

    // functions
    val SERIAL_DESC_FIELD_NAME = Name.identifier(SERIAL_DESC_FIELD)

    val SAVE_NAME = Name.identifier(SAVE)

    val LOAD_NAME = Name.identifier(LOAD)

    val CHILD_SERIALIZERS_GETTER = Name.identifier("childSerializers")

    val TYPE_PARAMS_SERIALIZERS_GETTER = Name.identifier("typeParametersSerializers")

    val WRITE_SELF_NAME = Name.identifier("write\$Self")

    val SERIALIZER_PROVIDER_NAME = Name.identifier("serializer")

    val SINGLE_MASK_FIELD_MISSING_FUNC_NAME = Name.identifier("throwMissingFieldException")

    val ARRAY_MASK_FIELD_MISSING_FUNC_NAME = Name.identifier("throwArrayMissingFieldException")

    val ENUM_SERIALIZER_FACTORY_FUNC_NAME = Name.identifier("createSimpleEnumSerializer")

    val ANNOTATED_ENUM_SERIALIZER_FACTORY_FUNC_NAME = Name.identifier("createAnnotatedEnumSerializer")

    val SINGLE_MASK_FIELD_MISSING_FUNC_FQ =
            SerializationPackages.internalPackageFqName.child(SINGLE_MASK_FIELD_MISSING_FUNC_NAME)

    val ARRAY_MASK_FIELD_MISSING_FUNC_FQ =
            SerializationPackages.internalPackageFqName.child(ARRAY_MASK_FIELD_MISSING_FUNC_NAME)

    val CACHED_SERIALIZER_PROPERTY_NAME = Name.identifier(CACHED_SERIALIZER_PROPERTY)

    val CACHED_CHILD_SERIALIZERS_PROPERTY_NAME = Name.identifier(CACHED_CHILD_SERIALIZERS_PROPERTY)

    val CACHED_DESCRIPTOR_FIELD_NAME = Name.identifier(CACHED_DESCRIPTOR_FIELD)

    // parameters
    val dummyParamName = Name.identifier("serializationConstructorMarker")

    const val typeArgPrefix = "typeSerial"

    val wrapIntoNullableExt = SerializationPackages.builtinsPackageFqName.child(Name.identifier("nullable"))

    val wrapIntoNullableCallableId =
            CallableId(SerializationPackages.builtinsPackageFqName, Name.identifier("nullable"))

}