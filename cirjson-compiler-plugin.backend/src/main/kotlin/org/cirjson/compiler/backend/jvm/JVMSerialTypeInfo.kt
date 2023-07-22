package org.cirjson.compiler.backend.jvm

import org.cirjson.compiler.backend.common.SerialTypeInfo
import org.cirjson.compiler.resolve.SerializableProperty
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.org.objectweb.asm.Type

class JVMSerialTypeInfo(property: SerializableProperty, val type: Type, nn: String,
        serializer: ClassDescriptor? = null) : SerialTypeInfo(property, nn, serializer)