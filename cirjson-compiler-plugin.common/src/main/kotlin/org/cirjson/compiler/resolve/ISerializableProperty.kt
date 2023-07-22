package org.cirjson.compiler.resolve

import org.jetbrains.kotlin.name.Name

interface ISerializableProperty {

    val isConstructorParameterWithDefault: Boolean

    val name: String

    val originalDescriptorName: Name

    val optional: Boolean

    val transient: Boolean

}