package org.cirjson.compiler.resolve

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

object SerializationAnnotations {

    // When changing names for these annotations, please change
    // org.jetbrains.kotlin.idea.caches.lightClasses.annotations.KOTLINX_SERIALIZABLE_FQ_NAME and
    // org.jetbrains.kotlin.idea.caches.lightClasses.annotations.KOTLINX_SERIALIZER_FQ_NAME accordingly.
    // Otherwise, there it might lead to exceptions from light classes when building them for serializer/serializable classes
    val serializableAnnotationFqName = FqName("org.cirjson.serialization.CircularSerializable")

    val serializerAnnotationFqName = FqName("org.cirjson.serialization.CircularSerializer")

    val serialNameAnnotationFqName = FqName("org.cirjson.serialization.SerialName")

    val requiredAnnotationFqName = FqName("org.cirjson.serialization.Required")

    val serialTransientFqName = FqName("org.cirjson.serialization.Transient")

    // Also implicitly used in kotlin-native.compiler.backend.native/CodeGenerationInfo.kt
    val serialInfoFqName = FqName("org.cirjson.serialization.SerialInfo")

    val inheritableSerialInfoFqName = FqName("org.cirjson.serialization.InheritableCircularSerialInfo")

    val metaSerializableAnnotationFqName = FqName("org.cirjson.serialization.MetaCircularSerializable")

    val encodeDefaultFqName = FqName("org.cirjson.serialization.EncodeDefault")

    val contextualFqName = FqName("org.cirjson.serialization.ContextualSerialization") // this one is deprecated

    val contextualOnFileFqName = FqName("org.cirjson.serialization.UseContextualCircularSerialization")

    val contextualOnPropertyFqName = FqName("org.cirjson.serialization.Contextual")

    val polymorphicFqName = FqName("org.cirjson.serialization.Polymorphic")

    val additionalSerializersFqName = FqName("org.cirjson.serialization.UseCircularSerializers")

    val serializableAnnotationClassId = ClassId.topLevel(serializableAnnotationFqName)

    val serializerAnnotationClassId = ClassId.topLevel(serializerAnnotationFqName)

    val serialNameAnnotationClassId = ClassId.topLevel(serialNameAnnotationFqName)

    val requiredAnnotationClassId = ClassId.topLevel(requiredAnnotationFqName)

    val serialTransientClassId = ClassId.topLevel(serialTransientFqName)

    val serialInfoClassId = ClassId.topLevel(serialInfoFqName)

    val inheritableSerialInfoClassId = ClassId.topLevel(inheritableSerialInfoFqName)

    val metaSerializableAnnotationClassId = ClassId.topLevel(metaSerializableAnnotationFqName)

    val encodeDefaultClassId = ClassId.topLevel(encodeDefaultFqName)

    val contextualClassId = ClassId.topLevel(contextualFqName)

    val contextualOnFileClassId = ClassId.topLevel(contextualOnFileFqName)

    val contextualOnPropertyClassId = ClassId.topLevel(contextualOnPropertyFqName)

    val polymorphicClassId = ClassId.topLevel(polymorphicFqName)

    val additionalSerializersClassId = ClassId.topLevel(additionalSerializersFqName)

}