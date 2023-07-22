package org.cirjson.compiler.resolve

interface ISerializableProperties<S : ISerializableProperty> {

    val serializableProperties: List<S>

    val isExternallySerializable: Boolean

    val serializableConstructorProperties: List<S>

    val serializableStandaloneProperties: List<S>

}