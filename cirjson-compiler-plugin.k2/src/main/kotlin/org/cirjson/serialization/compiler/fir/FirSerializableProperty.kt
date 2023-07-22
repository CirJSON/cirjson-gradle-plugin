package org.cirjson.serialization.compiler.fir

import org.cirjson.compiler.resolve.ISerializableProperty
import org.cirjson.serialization.compiler.fir.services.analyzeSpecialSerializers
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.Name

class FirSerializableProperty(session: FirSession, val propertySymbol: FirPropertySymbol,
        override val isConstructorParameterWithDefault: Boolean, declaresDefaultValue: Boolean) :
    ISerializableProperty {

    override val name: String = propertySymbol.getSerialNameValue(session) ?: propertySymbol.name.asString()

    override val originalDescriptorName: Name
        get() = propertySymbol.name

    override val optional: Boolean = !propertySymbol.getSerialRequired(session) && declaresDefaultValue

    override val transient: Boolean = propertySymbol.hasSerialTransient(session) || !propertySymbol.hasBackingField

    val serializableWith: ConeKotlinType? =
        propertySymbol.getSerializableWith(session) ?: analyzeSpecialSerializers(session,
                propertySymbol.resolvedAnnotationsWithArguments)?.defaultType()

}