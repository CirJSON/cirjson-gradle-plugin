package org.cirjson.compiler.diagnostic

import org.cirjson.compiler.resolve.*
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.util.slicedMap.Slices
import org.jetbrains.kotlin.util.slicedMap.WritableSlice

val SERIALIZABLE_PROPERTIES: WritableSlice<ClassDescriptor, SerializableProperties> = Slices.createSimpleSlice()

val ClassDescriptor.serializableAnnotationIsUseless: Boolean
    get() = hasSerializableOrMetaAnnotationWithoutArgs && !isInternalSerializable && !hasCompanionObjectAsSerializer && kind != ClassKind.ENUM_CLASS && !isSealedSerializableInterface
