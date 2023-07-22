package org.cirjson.serialization.compiler.fir.services

import org.cirjson.compiler.resolve.SerialEntityNames.ANNOTATED_ENUM_SERIALIZER_FACTORY_FUNC_NAME
import org.cirjson.compiler.resolve.SerialEntityNames.ENUM_SERIALIZER_FACTORY_FUNC_NAME
import org.cirjson.compiler.resolve.SerializationPackages
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class DependencySerializationInfoProvider(session: FirSession) : FirExtensionSessionComponent(session) {

    val useGeneratedEnumSerializer by session.firCachesFactory.createLazyValue {
        val enumSerializerFactory =
            session.symbolProvider.getTopLevelFunctionSymbols(SerializationPackages.internalPackageFqName,
                    ENUM_SERIALIZER_FACTORY_FUNC_NAME)

        val annotatedEnumSerializerFactory =
            session.symbolProvider.getTopLevelFunctionSymbols(SerializationPackages.internalPackageFqName,
                    ANNOTATED_ENUM_SERIALIZER_FACTORY_FUNC_NAME)

        enumSerializerFactory.isEmpty() || annotatedEnumSerializerFactory.isEmpty()
    }

    private val classesFromSerializationPackageCache: FirCache<Name, FirClassSymbol<*>, Nothing?> =
        session.firCachesFactory.createCache { name ->
            SerializationPackages.allPublicPackages.firstNotNullOfOrNull { packageName ->
                session.symbolProvider.getClassLikeSymbolByClassId(ClassId(packageName, name)) as? FirClassSymbol<*>
            } ?: throw IllegalArgumentException("Can't locate cass ${name.identifier}")
        }

    fun getClassFromSerializationPackage(name: Name): FirClassSymbol<*> {
        return classesFromSerializationPackageCache.getValue(name)
    }

    private val classesFromInternalSerializationPackageCache: FirCache<Name, FirClassSymbol<*>, Nothing?> =
        session.firCachesFactory.createCache { name ->
            session.symbolProvider.getClassLikeSymbolByClassId(
                    ClassId(SerializationPackages.internalPackageFqName, name)) as? FirClassSymbol<*>
                ?: throw IllegalArgumentException("Can't locate cass ${name.identifier}")
        }

    fun getClassFromInternalSerializationPackage(name: Name): FirClassSymbol<*> {
        return classesFromInternalSerializationPackageCache.getValue(name)
    }

}