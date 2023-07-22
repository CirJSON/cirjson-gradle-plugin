package org.cirjson.compiler.extensions

import org.cirjson.compiler.backend.ir.SerializationBaseContext
import org.cirjson.compiler.backend.ir.enumEntries
import org.cirjson.compiler.backend.ir.getClassFromRuntimeOrNull
import org.cirjson.compiler.resolve.SerialEntityNames
import org.cirjson.compiler.resolve.SerializationDependencies
import org.cirjson.compiler.resolve.SerializationJsDependenciesClassIds
import org.cirjson.compiler.resolve.SerializationPackages
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.findDeclaration
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import java.util.concurrent.ConcurrentHashMap

class SerializationPluginContext(baseContext: IrPluginContext,
        val metadataPlugin: SerializationDescriptorSerializerPlugin?) : IrPluginContext by baseContext,
        SerializationBaseContext {

    internal val copiedStaticWriteSelf: MutableMap<IrSimpleFunction, IrSimpleFunction> = ConcurrentHashMap()

    // Kotlin built-in declarations
    internal val arrayValueGetter = irBuiltIns.arrayClass.owner.declarations.filterIsInstance<IrSimpleFunction>()
        .single { it.name.asString() == "get" }

    internal val intArrayOfFunctionSymbol = referenceFunctions(
            CallableId(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, Name.identifier("intArrayOf"))).first()

    // Kotlin stdlib declarations
    internal val jvmFieldClassSymbol = referenceClass(StandardClassIds.Annotations.JvmField)!!

    internal val lazyModeClass = referenceClass(ClassId.topLevel(SerializationDependencies.LAZY_MODE_FQ))!!.owner

    internal val lazyModePublicationEnumEntry =
            lazyModeClass.enumEntries().single { it.name == SerializationDependencies.LAZY_PUBLICATION_MODE_NAME }

    // There can be several transitive dependencies on kotlin-stdlib in IDE sources,
    // as well as several definitions of stdlib functions, including `kotlin.lazy`;
    // in that case `referenceFunctions` might return more than one valid definition of the same function.
    internal val lazyFunctionSymbol =
            referenceFunctions(CallableId(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, Name.identifier("lazy"))).first {
                it.owner.valueParameters.size == 2 && it.owner.valueParameters[0].type == lazyModeClass.defaultType
            }

    internal val lazyClass = referenceClass(ClassId.topLevel(SerializationDependencies.LAZY_FQ))!!.owner

    internal val lazyValueGetter = lazyClass.getPropertyGetter("value")!!

    internal val jsExportIgnoreClass: IrClass? by lazy {
        val pkg = SerializationJsDependenciesClassIds.jsExportIgnore.packageFqName
        val jsExportName = SerializationJsDependenciesClassIds.jsExportIgnore.parentClassId!!.shortClassName
        val jsExportIgnoreFqName = SerializationJsDependenciesClassIds.jsExportIgnore.asSingleFqName()

        getClassFromRuntimeOrNull(jsExportName.identifier,
                pkg)?.owner?.findDeclaration { it.fqNameWhenAvailable == jsExportIgnoreFqName }
    }

    // serialization runtime declarations
    internal val enumSerializerFactoryFunc = baseContext.referenceFunctions(
            CallableId(SerializationPackages.internalPackageFqName,
                    SerialEntityNames.ENUM_SERIALIZER_FACTORY_FUNC_NAME)).singleOrNull()

    internal val annotatedEnumSerializerFactoryFunc = baseContext.referenceFunctions(
            CallableId(SerializationPackages.internalPackageFqName,
                    SerialEntityNames.ANNOTATED_ENUM_SERIALIZER_FACTORY_FUNC_NAME)).singleOrNull()

    /**
     * @return `null` if there is no serialization runtime in the classpath
     */
    internal val kSerializerClass = referenceClass(SerialEntityNames.KSERIALIZER_CLASS_ID)?.owner

    // evaluated properties
    override val runtimeHasEnumSerializerFactoryFunctions =
            enumSerializerFactoryFunc != null && annotatedEnumSerializerFactoryFunc != null

    override fun referenceClassId(classId: ClassId): IrClassSymbol? = referenceClass(classId)

}