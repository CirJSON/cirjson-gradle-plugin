package org.cirjson.compiler.backend.js

import org.jetbrains.kotlin.js.backend.ast.JsSwitchMember

internal class JsCasesBuilder {

    val caseList: MutableList<JsSwitchMember> = mutableListOf()

    operator fun JsSwitchMember.unaryPlus() {
        caseList.add(this)
    }

}