package org.cirjson.compiler.backend.jvm

import org.cirjson.compiler.resolve.SerialEntityNames
import org.cirjson.compiler.resolve.SerialEntityNames.DECODER_CLASS
import org.cirjson.compiler.resolve.SerialEntityNames.ENCODER_CLASS
import org.cirjson.compiler.resolve.SerialEntityNames.ENUMS_FILE
import org.cirjson.compiler.resolve.SerialEntityNames.KSERIALIZER_CLASS
import org.cirjson.compiler.resolve.SerialEntityNames.MISSING_FIELD_EXC
import org.cirjson.compiler.resolve.SerialEntityNames.PLUGIN_EXCEPTIONS_FILE
import org.cirjson.compiler.resolve.SerialEntityNames.SERIAL_DESCRIPTOR_CLASS
import org.cirjson.compiler.resolve.SerialEntityNames.SERIAL_DESCRIPTOR_CLASS_IMPL
import org.cirjson.compiler.resolve.SerialEntityNames.SERIAL_DESCRIPTOR_FOR_ENUM
import org.cirjson.compiler.resolve.SerialEntityNames.SERIAL_DESC_FIELD
import org.cirjson.compiler.resolve.SerialEntityNames.SERIAL_EXC
import org.cirjson.compiler.resolve.SerialEntityNames.SERIAL_LOADER_CLASS
import org.cirjson.compiler.resolve.SerialEntityNames.SERIAL_SAVER_CLASS
import org.cirjson.compiler.resolve.SerialEntityNames.STRUCTURE_DECODER_CLASS
import org.cirjson.compiler.resolve.SerialEntityNames.STRUCTURE_ENCODER_CLASS
import org.cirjson.compiler.resolve.SerialEntityNames.UNKNOWN_FIELD_EXC
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.org.objectweb.asm.Type

internal val descType = Type.getObjectType("org/cirjson/serialization/descriptors/$SERIAL_DESCRIPTOR_CLASS")

internal val descImplType = Type.getObjectType("org/cirjson/serialization/internal/$SERIAL_DESCRIPTOR_CLASS_IMPL")

internal val descriptorForEnumsType =
    Type.getObjectType("org/cirjson/serialization/internal/$SERIAL_DESCRIPTOR_FOR_ENUM")

internal val generatedSerializerType =
    Type.getObjectType("org/cirjson/serialization/internal/${SerialEntityNames.GENERATED_SERIALIZER_CLASS}")

internal val kOutputType = Type.getObjectType("org/cirjson/serialization/encoding/$STRUCTURE_ENCODER_CLASS")

internal val encoderType = Type.getObjectType("org/cirjson/serialization/encoding/$ENCODER_CLASS")

internal val decoderType = Type.getObjectType("org/cirjson/serialization/encoding/$DECODER_CLASS")

internal val kInputType = Type.getObjectType("org/cirjson/serialization/encoding/$STRUCTURE_DECODER_CLASS")

internal val pluginUtilsType = Type.getObjectType("org/cirjson/serialization/internal/${PLUGIN_EXCEPTIONS_FILE}Kt")

internal val enumFactoriesType = Type.getObjectType("org/cirjson/serialization/internal/${ENUMS_FILE}Kt")

internal val jvmLambdaType = Type.getObjectType("kotlin/jvm/internal/Lambda")

internal val kotlinLazyType = Type.getObjectType("kotlin/Lazy")

internal val function0Type = Type.getObjectType("kotlin/jvm/functions/Function0")

internal val threadSafeModeType = Type.getObjectType("kotlin/LazyThreadSafetyMode")

internal val kSerialSaverType = Type.getObjectType("org/cirjson/serialization/$SERIAL_SAVER_CLASS")

internal val kSerialLoaderType = Type.getObjectType("org/cirjson/serialization/$SERIAL_LOADER_CLASS")

internal val kSerializerType = Type.getObjectType("org/cirjson/serialization/$KSERIALIZER_CLASS")

internal val kSerializerArrayType = Type.getObjectType("[Lorg/cirjson/serialization/$KSERIALIZER_CLASS;")

internal val serializationExceptionName = "org/cirjson/serialization/$SERIAL_EXC"

internal val serializationExceptionMissingFieldName = "org/cirjson/serialization/$MISSING_FIELD_EXC"

internal val serializationExceptionUnknownIndexName = "org/cirjson/serialization/$UNKNOWN_FIELD_EXC"

internal val annotationType = Type.getObjectType("java/lang/annotation/Annotation")

internal val annotationArrayType = Type.getObjectType("[${annotationType.descriptor}")

internal val doubleAnnotationArrayType = Type.getObjectType("[${annotationArrayType.descriptor}")

internal val stringType = AsmTypes.JAVA_STRING_TYPE

internal val stringArrayType = Type.getObjectType("[${stringType.descriptor}")

internal val descriptorGetterName = JvmAbi.getterName(SERIAL_DESC_FIELD)

internal val getLazyValueName = JvmAbi.getterName("value")

val OPT_MASK_TYPE: Type = Type.INT_TYPE

val OPT_MASK_BITS = 32

// :kludge: for stripped-down version of ASM inside kotlin-compiler-embeddable.jar
const val VOID = 0

const val BOOLEAN = 1

const val CHAR = 2

const val BYTE = 3

const val SHORT = 4

const val INT = 5

const val FLOAT = 6

const val LONG = 7

const val DOUBLE = 8

const val ARRAY = 9

const val OBJECT = 10