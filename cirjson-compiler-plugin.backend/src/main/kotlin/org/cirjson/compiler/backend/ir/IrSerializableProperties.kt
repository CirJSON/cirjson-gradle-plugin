package org.cirjson.compiler.backend.ir

import org.cirjson.compiler.resolve.ISerializableProperties

class IrSerializableProperties(override val serializableProperties: List<IrSerializableProperty>,
        override val isExternallySerializable: Boolean,
        override val serializableConstructorProperties: List<IrSerializableProperty>,
        override val serializableStandaloneProperties: List<IrSerializableProperty>) :
        ISerializableProperties<IrSerializableProperty>