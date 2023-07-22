package org.cirjson.serialization.compiler.fir

import org.cirjson.serialization.compiler.fir.checkers.FirSerializationCheckersComponent
import org.cirjson.serialization.compiler.fir.services.ContextualSerializersProvider
import org.cirjson.serialization.compiler.fir.services.DependencySerializationInfoProvider
import org.cirjson.serialization.compiler.fir.services.FirSerializablePropertiesProvider
import org.cirjson.serialization.compiler.fir.services.FirVersionReader
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class FirSerializationExtensionRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::SerializationFirResolveExtension
        +::SerializationFirSupertypesExtension
        +::FirSerializationCheckersComponent
        +::SerializationFirDeclarationsForMetadataProvider

        // services
        +::DependencySerializationInfoProvider
        +::FirSerializablePropertiesProvider
        +::FirVersionReader
        +::ContextualSerializersProvider
    }
}