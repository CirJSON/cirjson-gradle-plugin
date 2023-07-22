package org.cirjson.compiler.extensions

import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension

open class SerializationCodegenExtension @JvmOverloads constructor(
        val metadataPlugin: SerializationDescriptorSerializerPlugin? = null) : ExpressionCodegenExtension {

    override fun generateClassSyntheticParts(codegen: ImplementationBodyCodegen) {
        SerialInfoCodegenImpl.generateSerialInfoImplBody(codegen)
        SerializableCodegenImpl.generateSerializableExtensions(codegen)
        SerializerCodegenImpl.generateSerializerExtensions(codegen, metadataPlugin)
        SerializableCompanionCodegenImpl.generateSerializableExtensions(codegen)
    }

    override val shouldGenerateClassSyntheticPartsInLightClassesMode: Boolean
        get() = false

}