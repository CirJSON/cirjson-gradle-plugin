package org.cirjson.serialization.compiler.fir.services

import org.jetbrains.kotlin.fir.FirSession

val FirSession.versionReader: FirVersionReader by FirSession.sessionComponentAccessor()

val FirSession.serializablePropertiesProvider: FirSerializablePropertiesProvider by FirSession.sessionComponentAccessor()

val FirSession.dependencySerializationInfoProvider: DependencySerializationInfoProvider by FirSession.sessionComponentAccessor()

val FirSession.contextualSerializersProvider: ContextualSerializersProvider by FirSession.sessionComponentAccessor()
