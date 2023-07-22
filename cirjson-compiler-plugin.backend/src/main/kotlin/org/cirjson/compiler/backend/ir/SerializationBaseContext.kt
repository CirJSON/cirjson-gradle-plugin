package org.cirjson.compiler.backend.ir

import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.name.ClassId

interface SerializationBaseContext {

    fun referenceClassId(classId: ClassId): IrClassSymbol?

    val runtimeHasEnumSerializerFactoryFunctions: Boolean

}