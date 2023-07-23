package org.cirjson.compiler.extensions

import org.cirjson.compiler.diagnostic.SerializationPluginDeclarationChecker
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.platform.TargetPlatform

class SerializationPluginComponentContainerContributor : StorageComponentContainerContributor {

    override fun registerModuleComponents(container: StorageComponentContainer, platform: TargetPlatform,
            moduleDescriptor: ModuleDescriptor) {
        container.useInstance(SerializationPluginDeclarationChecker())
    }

}