package org.cirjson.serialization.compiler.fir.services

import org.cirjson.compiler.diagnostic.CommonVersionReader
import org.cirjson.compiler.diagnostic.RuntimeVersions
import org.cirjson.compiler.resolve.SerialEntityNames
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.utils.sourceElement
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider

class FirVersionReader(session: FirSession) : FirExtensionSessionComponent(session) {

    val runtimeVersions: RuntimeVersions? by session.firCachesFactory.createLazyValue lazy@{
        val markerClass = session.symbolProvider.getClassLikeSymbolByClassId(SerialEntityNames.KSERIALIZER_CLASS_ID)
            ?: return@lazy null
        CommonVersionReader.computeRuntimeVersions(markerClass.sourceElement)
    }

    val canSupportInlineClasses by session.firCachesFactory.createLazyValue lazy@{
        CommonVersionReader.canSupportInlineClasses(runtimeVersions)
    }

}