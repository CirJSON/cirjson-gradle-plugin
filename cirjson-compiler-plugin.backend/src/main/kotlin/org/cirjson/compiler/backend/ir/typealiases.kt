package org.cirjson.compiler.backend.ir

import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol

internal typealias FunctionWithArgs = Pair<IrFunctionSymbol, List<IrExpression>>
