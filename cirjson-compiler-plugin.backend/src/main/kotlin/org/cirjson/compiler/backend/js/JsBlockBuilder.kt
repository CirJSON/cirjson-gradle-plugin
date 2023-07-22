package org.cirjson.compiler.backend.js

import org.jetbrains.kotlin.js.backend.ast.JsBlock
import org.jetbrains.kotlin.js.backend.ast.JsStatement

internal class JsBlockBuilder {

    val block: JsBlock = JsBlock()

    operator fun JsStatement.unaryPlus() {
        block.statements.add(this)
    }

    val body: List<JsStatement>
        get() = block.statements

}