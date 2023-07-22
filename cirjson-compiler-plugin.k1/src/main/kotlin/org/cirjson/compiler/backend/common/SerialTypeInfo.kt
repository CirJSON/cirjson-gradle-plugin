package org.cirjson.compiler.backend.common

import org.cirjson.compiler.resolve.SerializableProperty
import org.jetbrains.kotlin.descriptors.ClassDescriptor

open class SerialTypeInfo(val property: SerializableProperty, val elementMethodPrefix: String,
        val serializer: ClassDescriptor? = null)