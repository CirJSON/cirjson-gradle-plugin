package org.cirjson.compiler.backend.ir

import org.cirjson.compiler.extensions.SerializationPluginContext
import org.cirjson.compiler.resolve.SerialEntityNames
import org.jetbrains.kotlin.backend.common.ir.addExtensionReceiver
import org.jetbrains.kotlin.backend.jvm.lower.JvmAnnotationImplementationTransformer
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.types.starProjectedType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class SerialInfoImplJvmIrGenerator(private val context: SerializationPluginContext,
        private val moduleFragment: IrModuleFragment) {

    private val javaLangClass = createClass(createPackage("java.lang"), "Class", ClassKind.CLASS)

    private val jvmName: IrClassSymbol =
            createClass(createPackage("kotlin.jvm"), "JvmName", ClassKind.ANNOTATION_CLASS) { klass ->
                klass.addConstructor().apply {
                    addValueParameter("name", context.irBuiltIns.stringType)
                }
            }

    private val kClassJava: IrPropertySymbol = IrFactoryImpl.buildProperty {
        name = Name.identifier("java")
    }.apply {
        parent = createClass(createPackage("kotlin.jvm"), "JvmClassMappingKt", ClassKind.CLASS).owner
        addGetter().apply {
            annotations =
                    listOf(IrConstructorCallImpl.fromSymbolOwner(jvmName.typeWith(), jvmName.constructors.single())
                        .apply {
                            putValueArgument(0, IrConstImpl.string(UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                                    context.irBuiltIns.stringType, "getJavaClass"))
                        })
            addExtensionReceiver(context.irBuiltIns.kClassClass.starProjectedType)
            returnType = javaLangClass.starProjectedType
        }
    }.symbol

    private val implementor =
            JvmAnnotationImplementationTransformer.AnnotationPropertyImplementor(context.irFactory, context.irBuiltIns,
                    context.symbols, javaLangClass, kClassJava.owner.getter!!.symbol, SERIALIZATION_PLUGIN_ORIGIN)

    fun generateImplementationFor(annotationClass: IrClass) {

        val properties = annotationClass.declarations.filterIsInstance<IrProperty>()

        val subclass = context.irFactory.buildClass {
            startOffset = UNDEFINED_OFFSET
            endOffset = UNDEFINED_OFFSET
            name = SerialEntityNames.IMPL_NAME
            origin = SERIALIZATION_PLUGIN_ORIGIN
            visibility = DescriptorVisibilities.PUBLIC
        }.apply {
            parent = annotationClass
            createImplicitParameterDeclarationWithWrappedDescriptor()
            superTypes = listOf(annotationClass.defaultType)
        }
        annotationClass.declarations.add(subclass)

        val ctor = subclass.addConstructor {
            startOffset = UNDEFINED_OFFSET
            endOffset = UNDEFINED_OFFSET
            visibility = DescriptorVisibilities.PUBLIC
        }

        implementor.implementAnnotationPropertiesAndConstructor(properties, subclass, ctor, null)
    }

    private fun createPackage(packageName: String): IrPackageFragment =
            IrExternalPackageFragmentImpl.createEmptyExternalPackageFragment(moduleFragment.descriptor,
                    FqName(packageName))

    private fun createClass(irPackage: IrPackageFragment, shortName: String, classKind: ClassKind,
            block: (IrClass) -> Unit = {}): IrClassSymbol = IrFactoryImpl.buildClass {
        name = Name.identifier(shortName)
        kind = classKind
        modality = Modality.FINAL
    }.apply {
        parent = irPackage
        createImplicitParameterDeclarationWithWrappedDescriptor()
        block(this)
    }.symbol

}