package org.cirjson.serialization.compiler.fir.services

import org.cirjson.serialization.compiler.fir.*
import org.cirjson.serialization.compiler.fir.checkers.getSuperClassNotAny
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.primaryConstructorSymbol
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.utils.correspondingValueParameterFromPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.scopes.processAllProperties
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.utils.addIfNotNull

class FirSerializablePropertiesProvider(session: FirSession) : FirExtensionSessionComponent(session) {

    private val cache: FirCache<FirClassSymbol<*>, FirSerializableProperties, Nothing?> =
        session.firCachesFactory.createCache(this::createSerializableProperties)

    fun getSerializablePropertiesForClass(classSymbol: FirClassSymbol<*>): FirSerializableProperties {
        return cache.getValue(classSymbol)
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(FirSerializationPredicates.hasMetaAnnotation)
    }

    private fun createSerializableProperties(classSymbol: FirClassSymbol<*>): FirSerializableProperties {
        val allPropertySymbols = buildList {
            classSymbol.declaredMemberScope(session).processAllProperties {
                addIfNotNull(it as? FirPropertySymbol)
            }
        }

        val primaryConstructorProperties = allPropertySymbols.mapNotNull {
            val parameterSymbol = it.correspondingValueParameterFromPrimaryConstructor ?: return@mapNotNull null
            it to parameterSymbol.hasDefaultValue
        }.toMap().withDefault { false }

        val isInternalSerializable = with(session) { classSymbol.isInternalSerializable }

        fun isPropertySerializable(propertySymbol: FirPropertySymbol): Boolean {
            return when {
                isInternalSerializable -> !propertySymbol.hasSerialTransient(session)
                propertySymbol.visibility == Visibilities.Private -> false
                else -> (propertySymbol.isVar && propertySymbol.hasSerialTransient(
                        session)) || propertySymbol in primaryConstructorProperties
            }
        }

        val serializableProperties: List<FirSerializableProperty> =
            allPropertySymbols.asSequence().filter { isPropertySerializable(it) }.map {
                val declaresDefaultValue = it.declaresDefaultValue()
                FirSerializableProperty(session, it, primaryConstructorProperties.getValue(it), declaresDefaultValue)
            }.filterNot { it.transient }.partition { it.propertySymbol in primaryConstructorProperties }
                    .let { (fromConstructor, standalone) ->
                        val superClassSymbol = classSymbol.getSuperClassNotAny(session)
                        buildList {
                            if (superClassSymbol != null && with(session) { superClassSymbol.isInternalSerializable }) {
                                addAll(getSerializablePropertiesForClass(superClassSymbol).serializableProperties)
                            }
                            addAll(fromConstructor)
                            addAll(standalone)
                        }
                    }.let { restoreCorrectOrderFromClassProtoExtension(classSymbol, it) }

        val isExternallySerializable =
            classSymbol.isEnumClass || primaryConstructorProperties.size == (classSymbol.primaryConstructorSymbol()?.valueParameterSymbols?.size
                ?: 0)

        val (serializableConstructorProperties, serializableStandaloneProperties) = serializableProperties.partition { it.propertySymbol in primaryConstructorProperties }
        return FirSerializableProperties(serializableProperties, isExternallySerializable,
                serializableConstructorProperties, serializableStandaloneProperties)
    }

}