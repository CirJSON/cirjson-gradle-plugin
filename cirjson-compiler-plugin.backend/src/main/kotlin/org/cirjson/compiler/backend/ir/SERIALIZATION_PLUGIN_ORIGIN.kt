package org.cirjson.compiler.backend.ir

import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl

@Suppress("ClassName")
object SERIALIZATION_PLUGIN_ORIGIN : IrDeclarationOriginImpl("CIRJSON_SERIALIZATION", true)
