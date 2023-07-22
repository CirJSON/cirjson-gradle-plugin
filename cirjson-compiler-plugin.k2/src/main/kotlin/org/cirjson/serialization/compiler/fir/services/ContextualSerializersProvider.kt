package org.cirjson.serialization.compiler.fir.services

import org.cirjson.compiler.resolve.SerializationAnnotations
import org.cirjson.serialization.compiler.fir.serializerForType
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.getTargetType
import org.jetbrains.kotlin.fir.expressions.FirArrayOfCall
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.name.ClassId

class ContextualSerializersProvider(session: FirSession) : FirExtensionSessionComponent(session) {

    private val contextualKClassListCache: FirCache<FirFile, Set<ConeKotlinType>, Nothing?> =
        session.firCachesFactory.createCache { file ->
            buildSet {
                addAll(getKClassListFromFileAnnotation(file, SerializationAnnotations.contextualClassId))
                addAll(getKClassListFromFileAnnotation(file, SerializationAnnotations.contextualOnFileClassId))
            }
        }

    fun getContextualKClassListForFile(file: FirFile): Set<ConeKotlinType> {
        return contextualKClassListCache.getValue(file)
    }

    private val additionalSerializersInScopeCache: FirCache<FirFile, Map<Pair<FirClassSymbol<*>, Boolean>, FirClassSymbol<*>>, Nothing?> =
        session.firCachesFactory.createCache { file ->
            getKClassListFromFileAnnotation(file, SerializationAnnotations.additionalSerializersClassId).associateBy(
                    keySelector = {
                        val serializerType = it.serializerForType(session)
                        val symbol = serializerType?.toRegularClassSymbol(session) ?: throw AssertionError(
                                "Argument for ${SerializationAnnotations.additionalSerializersFqName} does not implement KSerializer or does not provide serializer for concrete type")
                        symbol to serializerType.isMarkedNullable
                    }, valueTransform = { it.toRegularClassSymbol(session)!! })
        }

    fun getAdditionalSerializersInScopeForFile(
            file: FirFile): Map<Pair<FirClassSymbol<*>, Boolean>, FirClassSymbol<*>> {
        return additionalSerializersInScopeCache.getValue(file)
    }

    private fun getKClassListFromFileAnnotation(file: FirFile, annotationClassId: ClassId): List<ConeKotlinType> {
        val annotation = file.symbol.resolvedAnnotationsWithArguments.getAnnotationByClassId(annotationClassId, session)
            ?: return emptyList()
        val arguments = when (val argument = annotation.argumentMapping.mapping.values.firstOrNull()) {
            is FirArrayOfCall -> argument.arguments
            is FirVarargArgumentsExpression -> argument.arguments
            else -> return emptyList()
        }
        return arguments.mapNotNull { (it as? FirGetClassCall)?.getTargetType() }
    }

}