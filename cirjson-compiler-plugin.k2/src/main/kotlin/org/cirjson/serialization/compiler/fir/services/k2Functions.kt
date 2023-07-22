package org.cirjson.serialization.compiler.fir.services

import org.cirjson.compiler.resolve.*
import org.cirjson.serialization.compiler.fir.*
import org.cirjson.serialization.compiler.fir.checkers.classSerializer
import org.cirjson.serialization.compiler.fir.checkers.currentFile
import org.cirjson.serialization.compiler.fir.checkers.overriddenSerializer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.runIf

fun FirPropertySymbol.declaresDefaultValue(): Boolean {
    return hasInitializer
}

@Suppress("UNUSED_PARAMETER") fun <P : ISerializableProperty> restoreCorrectOrderFromClassProtoExtension(
        classSymbol: FirClassSymbol<*>, props: List<P>): List<P> {
    return props
}

context(CheckerContext)
fun findTypeSerializerOrContextUnchecked(type: ConeKotlinType): FirClassSymbol<*>? {
    if (type.isTypeParameter) return null
    val annotations = type.fullyExpandedType(session).customAnnotations
    annotations.getSerializableWith(session)?.let { return it.toRegularClassSymbol(session) }
    val classSymbol = type.toRegularClassSymbol(session) ?: return null
    val currentFile = currentFile
    val provider = session.contextualSerializersProvider
    provider.getAdditionalSerializersInScopeForFile(
            currentFile)[classSymbol to type.isMarkedNullable]?.let { return it }
    if (type.isMarkedNullable) {
        return findTypeSerializerOrContextUnchecked(type.withNullability(ConeNullability.NOT_NULL, session.typeContext))
    }
    if (type in provider.getContextualKClassListForFile(currentFile)) {
        return session.dependencySerializationInfoProvider.getClassFromSerializationPackage(
                SpecialBuiltins.Names.contextSerializer)
    }
    return analyzeSpecialSerializers(session, annotations) ?: findTypeSerializer(type)
}

/**
 * Returns class descriptor for ContextSerializer or PolymorphicSerializer
 * if [annotations] contains @Contextual or @Polymorphic annotation
 */
fun analyzeSpecialSerializers(session: FirSession, annotations: List<FirAnnotation>): FirClassSymbol<*>? = when {
    annotations.hasAnnotation(SerializationAnnotations.contextualClassId, session) || annotations.hasAnnotation(
            SerializationAnnotations.contextualOnPropertyClassId, session) -> {
        session.dependencySerializationInfoProvider.getClassFromSerializationPackage(
                SpecialBuiltins.Names.contextSerializer)
    }
    // can be annotation on type usage, e.g. List<@Polymorphic Any>
    annotations.hasAnnotation(SerializationAnnotations.polymorphicClassId, session) -> {
        session.dependencySerializationInfoProvider.getClassFromSerializationPackage(
                SpecialBuiltins.Names.polymorphicSerializer)
    }

    else -> null
}

context(CheckerContext)
fun findTypeSerializer(type: ConeKotlinType): FirClassSymbol<*>? {
    val userOverride = type.overriddenSerializer
    if (userOverride != null) return userOverride.toRegularClassSymbol(session)
    if (type.isTypeParameter) return null
    val serializationProvider = session.dependencySerializationInfoProvider
    if (type.isArrayType) {
        return serializationProvider.getClassFromInternalSerializationPackage(
                SpecialBuiltins.Names.referenceArraySerializer)
    }
    if (with(session) { type.isGeneratedSerializableObject }) {
        return serializationProvider.getClassFromInternalSerializationPackage(SpecialBuiltins.Names.objectSerializer)
    }
    // see if there is a standard serializer
    val standardSerializer = with(session) { findStandardKotlinTypeSerializer(type) ?: findEnumTypeSerializer(type) }
    if (standardSerializer != null) return standardSerializer
    val symbol = type.toRegularClassSymbol(session) ?: return null
    if (with(session) { symbol.isSealedSerializableInterface }) {
        return serializationProvider.getClassFromSerializationPackage(SpecialBuiltins.Names.polymorphicSerializer)
    }
    return symbol.classSerializer // check for serializer defined on the type
}

context(FirSession)
fun findStandardKotlinTypeSerializer(type: ConeKotlinType): FirClassSymbol<*>? {
    val name = when {
        type.isBoolean -> PrimitiveBuiltins.booleanSerializer
        type.isByte -> PrimitiveBuiltins.byteSerializer
        type.isShort -> PrimitiveBuiltins.shortSerializer
        type.isInt -> PrimitiveBuiltins.intSerializer
        type.isLong -> PrimitiveBuiltins.longSerializer
        type.isFloat -> PrimitiveBuiltins.floatSerializer
        type.isDouble -> PrimitiveBuiltins.doubleSerializer
        type.isChar -> PrimitiveBuiltins.charSerializer
        else -> findStandardKotlinTypeSerializerName(type.classId?.asFqNameString())
    }?.let(Name::identifier) ?: return null
    val symbolProvider = symbolProvider
    return symbolProvider.getClassLikeSymbolByClassId(
            ClassId(SerializationPackages.internalPackageFqName, name)) as? FirClassSymbol<*>
        ?: symbolProvider.getClassLikeSymbolByClassId(
                ClassId(SerializationPackages.packageFqName, name)) as? FirClassSymbol<*>
}

context(FirSession)
fun findEnumTypeSerializer(type: ConeKotlinType): FirClassSymbol<*>? {
    val symbol = type.toRegularClassSymbol(this@FirSession) ?: return null
    return runIf(symbol.isEnumClass && !symbol.isEnumWithLegacyGeneratedSerializer) {
        symbolProvider.getClassLikeSymbolByClassId(SerializersClassIds.enumSerializerId) as? FirClassSymbol<*>
    }
}
