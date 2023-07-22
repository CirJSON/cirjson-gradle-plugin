package org.cirjson.serialization.compiler.fir

import org.cirjson.compiler.resolve.ISerializableProperties

class FirSerializableProperties(override val serializableProperties: List<FirSerializableProperty>,
        override val isExternallySerializable: Boolean,
        override val serializableConstructorProperties: List<FirSerializableProperty>,
        override val serializableStandaloneProperties: List<FirSerializableProperty>) :
    ISerializableProperties<FirSerializableProperty>