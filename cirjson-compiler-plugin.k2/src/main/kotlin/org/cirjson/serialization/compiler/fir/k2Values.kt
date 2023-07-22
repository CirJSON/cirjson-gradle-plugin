package org.cirjson.serialization.compiler.fir

import org.cirjson.compiler.resolve.SerialEntityNames
import org.cirjson.compiler.resolve.SerializationAnnotations.inheritableSerialInfoClassId
import org.cirjson.compiler.resolve.SerializationAnnotations.metaSerializableAnnotationClassId
import org.cirjson.compiler.resolve.SerializationAnnotations.serialInfoClassId
import org.cirjson.serialization.compiler.fir.services.dependencySerializationInfoProvider
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.isWasm
import org.jetbrains.kotlin.platform.konan.isNative

// ---------------------- annotations utils ----------------------
context(FirSession)
val FirBasedSymbol<*>.isSerialInfoAnnotation: Boolean
    get() = hasAnnotation(serialInfoClassId, this@FirSession) || hasAnnotation(inheritableSerialInfoClassId,
            this@FirSession) || hasAnnotation(metaSerializableAnnotationClassId, this@FirSession)

context(FirSession)
val FirClassSymbol<*>.hasSerializableAnnotation: Boolean
    get() = serializableAnnotation(needArguments = false, this@FirSession) != null

// ---------------------- class utils ----------------------

context(FirSession)
internal val FirClassSymbol<*>.isInternallySerializableObject: Boolean
    get() = classKind.isObject && hasSerializableOrMetaAnnotationWithoutArgs

context(FirSession)
internal val FirClassSymbol<*>.isSerializableObject: Boolean
    get() = classKind.isObject && hasSerializableOrMetaAnnotation

context(FirSession)
internal val FirClassSymbol<*>.isSealedSerializableInterface: Boolean
    get() = classKind.isInterface && rawStatus.modality == Modality.SEALED && hasSerializableOrMetaAnnotation

context(FirSession)
val FirClassSymbol<*>.hasSerializableOrMetaAnnotation: Boolean
    get() = hasSerializableAnnotation || hasMetaSerializableAnnotation

context(FirSession)
val FirClassSymbol<*>.hasMetaSerializableAnnotation: Boolean
    get() = predicateBasedProvider.matches(FirSerializationPredicates.hasMetaAnnotation, this)

context(FirSession)
internal val FirClassSymbol<*>.shouldHaveGeneratedMethodsInCompanion: Boolean
    get() = isSerializableObject || isSerializableEnum || (classKind == ClassKind.CLASS && hasSerializableOrMetaAnnotation) || isSealedSerializableInterface

context(FirSession)
internal val FirClassSymbol<*>.companionNeedsSerializerFactory: Boolean
    get() {
        if (!moduleData.platform.run { isNative() || isJs() || isWasm() }) return false
        if (isSerializableObject) return true
        if (isSerializableEnum) return true
        if (isAbstractOrSealedSerializableClass) return true
        if (isSealedSerializableInterface) return true
        return typeParameterSymbols.isNotEmpty()
    }

context(FirSession)
internal val FirClassSymbol<*>.isInternalSerializable: Boolean
    get() {
        if (!classKind.isClass) return false
        return hasSerializableOrMetaAnnotationWithoutArgs
    }

context(FirSession)
val FirClassSymbol<*>.hasSerializableOrMetaAnnotationWithoutArgs: Boolean
    get() = hasSerializableAnnotationWithoutArgs(
            this@FirSession) || (!hasSerializableAnnotation && hasMetaSerializableAnnotation)

context(FirSession)
internal val FirClassSymbol<*>.isAbstractOrSealedSerializableClass: Boolean
    get() = isInternalSerializable && (rawStatus.modality == Modality.ABSTRACT || rawStatus.modality == Modality.SEALED)

/**
 * Check that class is enum and marked by `Serializable` or meta-serializable annotation.
 */
context(FirSession)
internal val FirClassSymbol<*>.isSerializableEnum: Boolean
    get() = classKind.isEnumClass && hasSerializableOrMetaAnnotation

context(FirSession)
val FirClassSymbol<*>.isEnumWithLegacyGeneratedSerializer: Boolean
    get() = classKind.isEnumClass && dependencySerializationInfoProvider.useGeneratedEnumSerializer && hasSerializableOrMetaAnnotationWithoutArgs

context(FirSession)
val FirClassSymbol<*>.shouldHaveGeneratedSerializer: Boolean
    get() = (isInternalSerializable && isFinalOrOpen()) || isEnumWithLegacyGeneratedSerializer

// ---------------------- type utils ----------------------

val ConeKotlinType.isKSerializer: Boolean
    get() = classId == SerialEntityNames.KSERIALIZER_CLASS_ID

val ConeKotlinType.isTypeParameter: Boolean
    get() = this is ConeTypeParameterType

context(FirSession)
val ConeKotlinType.isGeneratedSerializableObject: Boolean
    get() = toRegularClassSymbol(
            this@FirSession)?.let { it.classKind.isObject && it.hasSerializableOrMetaAnnotationWithoutArgs } ?: false

context(FirSession)
val ConeKotlinType.isAbstractOrSealedOrInterface: Boolean
    get() = toRegularClassSymbol(
            this@FirSession)?.let { it.classKind.isInterface || it.rawStatus.modality == Modality.ABSTRACT || it.rawStatus.modality == Modality.SEALED }
        ?: false