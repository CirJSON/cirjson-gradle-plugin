package org.cirjson.serialization.compiler.fir

import org.cirjson.compiler.resolve.SerializationAnnotations
import org.cirjson.compiler.resolve.SerializationAnnotations.inheritableSerialInfoClassId
import org.cirjson.compiler.resolve.SerializationAnnotations.serialNameAnnotationClassId
import org.cirjson.compiler.resolve.SerializationJsDependenciesClassIds
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationCall
import org.jetbrains.kotlin.fir.extensions.FirExtension
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.createSubstitutionForSupertype
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.substitution.ChainedSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

internal fun ConeKotlinType.makeNullableIfNotPrimitive(typeContext: ConeTypeContext): ConeKotlinType =
    if (isPrimitive) this else withNullability(ConeNullability.NULLABLE, typeContext)

// ---------------------- annotations utils ----------------------

fun FirBasedSymbol<*>.isInheritableSerialInfoAnnotation(session: FirSession): Boolean =
    hasAnnotation(inheritableSerialInfoClassId, session)

fun FirBasedSymbol<*>.getSerialNameAnnotation(session: FirSession): FirAnnotation? =
    resolvedAnnotationsWithArguments.getAnnotationByClassId(serialNameAnnotationClassId, session)

fun FirBasedSymbol<*>.getSerialNameValue(session: FirSession): String? =
    getSerialNameAnnotation(session)?.getStringArgument(AnnotationParameterNames.VALUE)

fun FirBasedSymbol<*>.getSerialRequired(session: FirSession): Boolean =
    hasAnnotation(SerializationAnnotations.requiredAnnotationClassId, session)

fun FirBasedSymbol<*>.hasSerialTransient(session: FirSession): Boolean = getSerialTransientAnnotation(session) != null

fun FirBasedSymbol<*>.getSerialTransientAnnotation(session: FirSession): FirAnnotation? =
    getAnnotationByClassId(SerializationAnnotations.serialTransientClassId, session)

fun FirBasedSymbol<*>.serializableAnnotation(needArguments: Boolean, session: FirSession): FirAnnotation? {
    val annotations = if (needArguments) {
        resolvedAnnotationsWithArguments
    } else {
        resolvedCompilerAnnotationsWithClassIds
    }
    return annotations.serializableAnnotation(session)
}

fun List<FirAnnotation>.serializableAnnotation(session: FirSession): FirAnnotation? {
    return getAnnotationByClassId(SerializationAnnotations.serializableAnnotationClassId, session)
}

fun FirClassSymbol<*>.hasSerializableAnnotationWithoutArgs(session: FirSession): Boolean =
    serializableAnnotation(needArguments = false, session)?.let {
        if (it is FirAnnotationCall) {
            it.arguments.isEmpty()
        } else {
            it.argumentMapping.mapping.isEmpty()
        }
    } ?: false

internal fun FirBasedSymbol<*>.getSerializableWith(session: FirSession): ConeKotlinType? =
    serializableAnnotation(needArguments = true, session)?.getKClassArgument(AnnotationParameterNames.WITH)

internal fun List<FirAnnotation>.getSerializableWith(session: FirSession): ConeKotlinType? =
    serializableAnnotation(session)?.getKClassArgument(AnnotationParameterNames.WITH)

fun FirAnnotation.getGetKClassArgument(name: Name): FirGetClassCall? {
    return findArgumentByName(name) as? FirGetClassCall
}

internal fun FirClassSymbol<*>.getSerializerAnnotation(session: FirSession): FirAnnotation? =
    getAnnotationByClassId(SerializationAnnotations.serializerAnnotationClassId, session)

// ---------------------- class utils ----------------------

internal fun FirClassSymbol<*>.getSerializerForClass(session: FirSession): ConeKotlinType? =
    resolvedAnnotationsWithArguments.getAnnotationByClassId(SerializationAnnotations.serializerAnnotationClassId,
                    session)?.getKClassArgument(AnnotationParameterNames.FOR_CLASS)

internal fun FirClassLikeDeclaration.getSerializerFor(session: FirSession): FirGetClassCall? =
    getAnnotationByClassId(SerializationAnnotations.serializerAnnotationClassId, session)?.getGetKClassArgument(
                    AnnotationParameterNames.FOR_CLASS)

internal fun FirClassSymbol<*>.isFinalOrOpen(): Boolean {
    val modality = rawStatus.modality
    // null means default modality, final
    return (modality == null || modality == Modality.FINAL || modality == Modality.OPEN)
}

// ---------------------- type utils ----------------------

fun ConeKotlinType.serializerForType(session: FirSession): ConeKotlinType? {
    return this.fullyExpandedType(session).toRegularClassSymbol(session)?.getAllSubstitutedSupertypes(session)
            ?.find { it.isKSerializer }?.typeArguments?.firstOrNull()?.type
}

fun FirRegularClassSymbol.getAllSubstitutedSupertypes(session: FirSession): Set<ConeKotlinType> {
    val result = mutableSetOf<ConeKotlinType>()

    fun process(symbol: FirRegularClassSymbol, substitutor: ConeSubstitutor) {
        for (superType in symbol.resolvedSuperTypes) {
            if (result.add(substitutor.substituteOrSelf(superType))) {
                val superClassSymbol = superType.fullyExpandedType(session).toRegularClassSymbol(session) ?: continue
                val superSubstitutor =
                    (superType as? ConeLookupTagBasedType)?.let { createSubstitutionForSupertype(it, session) }
                        ?: ConeSubstitutor.Empty
                process(superClassSymbol, ChainedSubstitutor(superSubstitutor, substitutor))
            }
        }
    }

    process(this, ConeSubstitutor.Empty)
    return result
}


context(FirExtension)
fun FirAnnotationContainer.excludeFromJsExport() {
    if (!session.moduleData.platform.isJs()) {
        return
    }
    val jsExportIgnore =
        session.symbolProvider.getClassLikeSymbolByClassId(SerializationJsDependenciesClassIds.jsExportIgnore)
    val jsExportIgnoreAnnotation = jsExportIgnore as? FirRegularClassSymbol ?: return
    val jsExportIgnoreConstructor =
        jsExportIgnoreAnnotation.declarationSymbols.firstIsInstanceOrNull<FirConstructorSymbol>() ?: return

    val jsExportIgnoreAnnotationCall = buildAnnotationCall {
        argumentList = buildResolvedArgumentList(linkedMapOf())
        annotationTypeRef = buildResolvedTypeRef {
            type = jsExportIgnoreAnnotation.defaultType()
        }
        calleeReference = buildResolvedNamedReference {
            name = jsExportIgnoreAnnotation.name
            resolvedSymbol = jsExportIgnoreConstructor
        }
    }

    replaceAnnotations(annotations + jsExportIgnoreAnnotationCall)
}