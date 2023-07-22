package org.cirjson.compiler.resolve

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object SerializationDependencies {

    val LAZY_FQ = FqName("kotlin.Lazy")

    val LAZY_FUNC_FQ = FqName("kotlin.lazy")

    val LAZY_MODE_FQ = FqName("kotlin.LazyThreadSafetyMode")

    val FUNCTION0_FQ = FqName("kotlin.Function0")

    val LAZY_PUBLICATION_MODE_NAME = Name.identifier("PUBLICATION")

}