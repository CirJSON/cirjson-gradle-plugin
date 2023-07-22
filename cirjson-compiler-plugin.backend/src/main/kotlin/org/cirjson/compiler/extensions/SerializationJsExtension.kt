package org.cirjson.compiler.extensions

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.declaration.DeclarationBodyVisitor
import org.jetbrains.kotlin.js.translate.extensions.JsSyntheticTranslateExtension
import org.jetbrains.kotlin.psi.KtPureClassOrObject

open class SerializationJsExtension @JvmOverloads constructor(
        val metadataPlugin: SerializationDescriptorSerializerPlugin? = null) : JsSyntheticTranslateExtension {

    override fun generateClassSyntheticParts(declaration: KtPureClassOrObject, descriptor: ClassDescriptor,
            translator: DeclarationBodyVisitor, context: TranslationContext) {
        SerializerJsTranslator.translate(descriptor, translator, context, metadataPlugin)
        SerializableJsTranslator.translate(declaration, descriptor, context)
        SerializableCompanionJsTranslator.translate(descriptor, translator, context)
    }

}