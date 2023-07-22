package org.cirjson.compiler.resolve

import org.jetbrains.kotlin.name.FqName

object SerializationPackages {

    val packageFqName = FqName("org.cirjson.serialization")

    val internalPackageFqName = FqName("org.cirjson.serialization.internal")

    val encodingPackageFqName = FqName("org.cirjson.serialization.encoding")

    val descriptorsPackageFqName = FqName("org.cirjson.serialization.descriptors")

    val builtinsPackageFqName = FqName("org.cirjson.serialization.builtins")

    val allPublicPackages =
            listOf(packageFqName, encodingPackageFqName, descriptorsPackageFqName, builtinsPackageFqName)

}

