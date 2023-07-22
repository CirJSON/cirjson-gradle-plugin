package org.cirjson.compiler.backend.ir

import org.jetbrains.kotlin.ir.symbols.IrClassSymbol

class IrSerialTypeInfo(val property: IrSerializableProperty, val elementMethodPrefix: String,
        val serializer: IrClassSymbol? = null)