package org.cirjson.compiler.resolve

import org.jetbrains.kotlin.name.Name

object SpecialBuiltins {

    const val referenceArraySerializer = "CircularReferenceArraySerializer"

    const val objectSerializer = "CircularObjectSerializer"

    const val enumSerializer = "CircularEnumSerializer"

    const val polymorphicSerializer = "PolymorphicCircularSerializer"

    const val sealedSerializer = "SealedClassCircularSerializer"

    const val contextSerializer = "ContextualCircularSerializer"

    const val nullableSerializer = "CircularNullableSerializer"

    object Names {

        val referenceArraySerializer = Name.identifier(SpecialBuiltins.referenceArraySerializer)

        val objectSerializer = Name.identifier(SpecialBuiltins.objectSerializer)

        val enumSerializer = Name.identifier(SpecialBuiltins.enumSerializer)

        val polymorphicSerializer = Name.identifier(SpecialBuiltins.polymorphicSerializer)

        val sealedSerializer = Name.identifier(SpecialBuiltins.sealedSerializer)

        val contextSerializer = Name.identifier(SpecialBuiltins.contextSerializer)

        val nullableSerializer = Name.identifier(SpecialBuiltins.nullableSerializer)

    }

}