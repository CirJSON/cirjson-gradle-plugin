package org.cirjson.compiler.backend.ir

import org.cirjson.compiler.extensions.SerializationDescriptorSerializerPlugin
import org.cirjson.compiler.extensions.SerializationPluginContext
import org.cirjson.compiler.fir.SerializationPluginKey
import org.cirjson.compiler.resolve.*
import org.cirjson.compiler.resolve.SerializersClassIds.contextSerializerId
import org.cirjson.compiler.resolve.SerializersClassIds.enumSerializerId
import org.cirjson.compiler.resolve.SerializersClassIds.objectSerializerId
import org.cirjson.compiler.resolve.SerializersClassIds.polymorphicSerializerId
import org.cirjson.compiler.resolve.SerializersClassIds.referenceArraySerializerId
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.ir.representativeUpperBound
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyProperty
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.deepCopyWithVariables
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.isWasm
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor

fun IrBuilderWithScope.getProperty(receiver: IrExpression, property: IrProperty): IrExpression {
    return if (property.getter != null) irGet(property.getter!!.returnType, receiver, property.getter!!.symbol)
    else irGetField(receiver, property.backingField!!)
}

/*
  Create a function that creates `get property value expressions` for given corresponded constructor's param
    (constructor_params) -> get_property_value_expression
 */
fun IrBuilderWithScope.createPropertyByParamReplacer(irClass: IrClass, serialProperties: List<IrSerializableProperty>,
        instance: IrValueParameter): (ValueParameterDescriptor) -> IrExpression? {
    fun IrSerializableProperty.irGet(): IrExpression {
        val ownerType = instance.symbol.owner.type
        return getProperty(irGet(type = ownerType, variable = instance.symbol), ir)
    }

    val serialPropertiesMap = serialProperties.associateBy { it.ir }

    val transientPropertiesSet =
            irClass.declarations.asSequence().filterIsInstance<IrProperty>().filter { it.backingField != null }
                .filter { !serialPropertiesMap.containsKey(it) }.toSet()

    return { vpd ->
        val propertyDescriptor = irClass.properties.find { it.name == vpd.name }
        if (propertyDescriptor != null) {
            val value = serialPropertiesMap[propertyDescriptor]
            value?.irGet() ?: run {
                if (propertyDescriptor in transientPropertiesSet) getProperty(irGet(instance), propertyDescriptor)
                else null
            }
        } else {
            null
        }
    }
}

/*
    Creates an initializer adapter function that can replace IR expressions of getting constructor parameter value by some other expression.
    Also adapter may replace IR expression of getting `this` value by another expression.
     */
@OptIn(ObsoleteDescriptorBasedAPI::class)
fun createInitializerAdapter(irClass: IrClass, paramGetReplacer: (ValueParameterDescriptor) -> IrExpression?,
        thisGetReplacer: Pair<IrValueSymbol, () -> IrExpression>? = null): (IrExpressionBody) -> IrExpression {
    val initializerTransformer = object : IrElementTransformerVoid() {

        // try to replace `get some value` expression
        override fun visitGetValue(expression: IrGetValue): IrExpression {
            val symbol = expression.symbol
            if (thisGetReplacer != null && thisGetReplacer.first == symbol) { // replace `get this value` expression
                return thisGetReplacer.second()
            }

            val descriptor = symbol.descriptor
            if (descriptor is ValueParameterDescriptor) { // replace `get parameter value` expression
                paramGetReplacer(descriptor)?.let { return it }
            }

            // otherwise leave expression as it is
            return super.visitGetValue(expression)
        }

    }
    val defaultsMap = extractDefaultValuesFromConstructor(irClass)
    return fun(initializer: IrExpressionBody): IrExpression {
        val rawExpression = initializer.expression
        val expression = if (rawExpression.isInitializePropertyFromParameter()) {
            defaultsMap.getValue(
                    (rawExpression as IrGetValue).symbol)!! // this is a primary constructor property, use corresponding default of value parameter
        } else {
            rawExpression
        }
        return expression.deepCopyWithVariables().transform(initializerTransformer, null)
    }
}

private fun extractDefaultValuesFromConstructor(irClass: IrClass?): Map<IrValueSymbol, IrExpression?> {
    if (irClass == null) return emptyMap()
    val original = irClass.constructors.singleOrNull { it.isPrimary } // default arguments of original constructor
    val defaultsMap: Map<IrValueSymbol, IrExpression?> =
            original?.valueParameters?.associate { it.symbol to it.defaultValue?.expression } ?: emptyMap()
    return defaultsMap + extractDefaultValuesFromConstructor(irClass.getSuperClassNotAny())
}

fun IrPluginContext.generateBodyForDefaultConstructor(declaration: IrConstructor): IrBody? {
    val type = declaration.returnType as? IrSimpleType ?: return null

    val delegatingAnyCall = IrDelegatingConstructorCallImpl(-1, -1, irBuiltIns.anyType,
            irBuiltIns.anyClass.owner.primaryConstructor?.symbol ?: return null, typeArgumentsCount = 0,
            valueArgumentsCount = 0)

    val initializerCall =
            IrInstanceInitializerCallImpl(-1, -1, (declaration.parent as? IrClass)?.symbol ?: return null, type)

    return irFactory.createBlockBody(-1, -1, listOf(delegatingAnyCall, initializerCall))
}

fun IrClass.addDefaultConstructorBodyIfAbsent(ctx: IrPluginContext) {
    val declaration = primaryConstructor ?: return
    if (declaration.body == null) declaration.body = ctx.generateBodyForDefaultConstructor(declaration)
}

internal fun IrType.isKSerializer(): Boolean {
    val simpleType = this as? IrSimpleType ?: return false
    val classifier = simpleType.classifier as? IrClassSymbol ?: return false
    val fqName = classifier.owner.fqNameWhenAvailable
    return fqName == SerialEntityNames.KSERIALIZER_NAME_FQ || fqName == SerialEntityNames.GENERATED_SERIALIZER_FQ
}

internal fun IrType.isGeneratedKSerializer(): Boolean =
        classifierOrNull?.isClassWithFqName(SerialEntityNames.GENERATED_SERIALIZER_FQ.toUnsafe()) == true

internal fun IrClass.findPluginGeneratedMethod(name: String, afterK2: Boolean): IrSimpleFunction? {
    return this.functions.find {
        it.name.asString() == name && it.isFromPlugin(afterK2)
    }
}

internal fun IrClass.isEnumWithLegacyGeneratedSerializer(): Boolean {
    return findEnumLegacySerializer() != null
}

internal fun IrClass.findEnumLegacySerializer(): IrClass? {
    return if (kind == ClassKind.ENUM_CLASS) {
        declarations.filterIsInstance<IrClass>().singleOrNull { it.name == SerialEntityNames.SERIALIZER_CLASS_NAME }
    } else {
        null
    }
}

internal fun IrClass.isInternallySerializableEnum(): Boolean =
        kind == ClassKind.ENUM_CLASS && hasSerializableOrMetaAnnotationWithoutArgs()

fun IrType.isGeneratedSerializableObject(): Boolean {
    return classOrNull?.run { owner.kind == ClassKind.OBJECT && owner.hasSerializableOrMetaAnnotationWithoutArgs() } == true
}

internal val IrClass.isSerializableObject: Boolean
    get() = kind == ClassKind.OBJECT && hasSerializableOrMetaAnnotation()

internal fun IrClass.hasSerializableOrMetaAnnotationWithoutArgs(): Boolean =
        checkSerializableOrMetaAnnotationArgs(mustDoNotHaveArgs = true)

fun IrClass.hasSerializableOrMetaAnnotation() = checkSerializableOrMetaAnnotationArgs(mustDoNotHaveArgs = false)

private fun IrClass.checkSerializableOrMetaAnnotationArgs(mustDoNotHaveArgs: Boolean): Boolean {
    val annot = getAnnotation(SerializationAnnotations.serializableAnnotationFqName)
    if (annot != null) { // @Serializable have higher priority
        if (!mustDoNotHaveArgs) return true
        return annot.getValueArgument(0) == null
    }
    return annotations.map { it.constructedClass.annotations }
        .any { it.hasAnnotation(SerializationAnnotations.metaSerializableAnnotationFqName) }
}

internal fun IrClass.shouldHaveGeneratedSerializer(): Boolean =
        (isInternalSerializable && (modality == Modality.FINAL || modality == Modality.OPEN)) || isEnumWithLegacyGeneratedSerializer()

internal fun IrClass.isSerializableEnum(): Boolean = kind == ClassKind.ENUM_CLASS && hasSerializableOrMetaAnnotation()

fun IrType.serialName(): String = this.classOrUpperBound()!!.owner.serialName()

fun IrClass.serialName(): String {
    return annotations.serialNameValue ?: fqNameWhenAvailable?.asString() ?: error(
            "${this.render()} does not have fqName")
}

fun IrClass.findEnumValuesMethod() = this.functions.singleOrNull { f ->
    f.name == Name.identifier(
            "values") && f.valueParameters.isEmpty() && f.extensionReceiverParameter == null && f.dispatchReceiverParameter == null
} ?: error("Enum class does not have single .values() function")

internal fun IrClass.enumEntries(): List<IrEnumEntry> {
    check(this.kind == ClassKind.ENUM_CLASS)
    return declarations.filterIsInstance<IrEnumEntry>().toList()
}

internal fun IrClass.isEnumWithSerialInfoAnnotation(): Boolean {
    if (kind != ClassKind.ENUM_CLASS) return false
    if (annotations.hasAnySerialAnnotation) return true
    return enumEntries().any { (it.annotations.hasAnySerialAnnotation) }
}

fun IrClass.findWriteSelfMethod(): IrSimpleFunction? =
        functions.singleOrNull { it.name == SerialEntityNames.WRITE_SELF_NAME && !it.isFakeOverride }

fun IrClass.getSuperClassNotAny(): IrClass? {
    val parentClass = superTypes.mapNotNull { it.classOrNull?.owner }
        .singleOrNull { it.kind == ClassKind.CLASS || it.kind == ClassKind.ENUM_CLASS } ?: return null
    return if (parentClass.defaultType.isAny()) null else parentClass
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
internal fun IrDeclaration.isFromPlugin(afterK2: Boolean): Boolean = if (afterK2) {
    this.origin == IrDeclarationOrigin.GeneratedByPlugin(SerializationPluginKey)
} else { // old FE doesn't specify custom origin
    (this.descriptor as? CallableMemberDescriptor)?.kind == CallableMemberDescriptor.Kind.SYNTHESIZED
}

internal fun IrConstructor.isSerializationCtor(): Boolean {/*kind == CallableMemberDescriptor.Kind.SYNTHESIZED does not work because DeserializedClassConstructorDescriptor loses its kind*/
    return valueParameters.lastOrNull()?.run {
        name == SerialEntityNames.dummyParamName && type.classFqName == SerializationPackages.internalPackageFqName.child(
                SerialEntityNames.SERIAL_CTOR_MARKER_NAME)
    } == true
}

internal fun IrConstructor.lastArgumentIsAnnotationArray(): Boolean {
    val lastArgType = valueParameters.lastOrNull()?.type
    if (lastArgType == null || !lastArgType.isArray()) return false
    return ((lastArgType as? IrSimpleType)?.arguments?.firstOrNull()?.typeOrNull?.classFqName?.toString() == "kotlin.Annotation")
}

fun IrClass.findSerializableSyntheticConstructor(): IrConstructorSymbol? {
    return declarations.filterIsInstance<IrConstructor>().singleOrNull { it.isSerializationCtor() }?.symbol
}

internal fun IrClass.needSerializerFactory(compilerContext: SerializationPluginContext): Boolean {
    if (!(compilerContext.platform?.isNative() == true || compilerContext.platform.isJs() || compilerContext.platform.isWasm())) return false
    val serializableClass = getSerializableClassDescriptorByCompanion(this) ?: return false
    if (serializableClass.isSerializableObject) return true
    if (serializableClass.isSerializableEnum()) return true
    if (serializableClass.isAbstractOrSealedSerializableClass) return true
    if (serializableClass.isSealedSerializableInterface) return true
    return serializableClass.typeParameters.isNotEmpty()
}

internal fun getSerializableClassDescriptorByCompanion(companion: IrClass): IrClass? {
    if (companion.isSerializableObject) return companion
    if (!companion.isCompanion) return null
    val classDescriptor = (companion.parent as? IrClass) ?: return null
    if (!classDescriptor.shouldHaveGeneratedMethodsInCompanion) return null
    return classDescriptor
}

internal fun IrExpression.isInitializePropertyFromParameter(): Boolean =
        this is IrGetValueImpl && this.origin == IrStatementOrigin.INITIALIZE_PROPERTY_FROM_PARAMETER

/**
 * True — ALWAYS
 * False — NEVER
 * null — not specified
 */
fun IrProperty.getEncodeDefaultAnnotationValue(): Boolean? {
    val call = annotations.findAnnotation(SerializationAnnotations.encodeDefaultFqName) ?: return null
    val arg = call.getValueArgument(0) ?: return true // ALWAYS by default
    val argValue = (arg as? IrGetEnumValue ?: error(
            "Argument of enum constructor expected to implement IrGetEnumValue, got $arg")).symbol.owner.name.toString()
    return when (argValue) {
        "ALWAYS" -> true
        "NEVER" -> false
        else -> error("Unknown EncodeDefaultMode enum value: $argValue")
    }
}

fun findSerializerConstructorForTypeArgumentsSerializers(serializer: IrClass): IrConstructorSymbol? {
    val typeParamsCount =
            ((serializer.superTypes.find { it.isKSerializer() } as IrSimpleType).arguments.first().typeOrNull!! as IrSimpleType).arguments.size
    if (typeParamsCount == 0) return null //don't need it

    return serializer.constructors.singleOrNull {
        it.valueParameters.let { vps -> vps.size == typeParamsCount && vps.all { vp -> vp.type.isKSerializer() } }
    }?.symbol
}

fun IrType.classOrUpperBound(): IrClassSymbol? = when (val cls = classifierOrNull) {
    is IrClassSymbol -> cls
    is IrScriptSymbol -> cls.owner.targetClass
    is IrTypeParameterSymbol -> cls.owner.representativeUpperBound.classOrUpperBound()
    null -> null
    else -> null
}

/**
 * Replaces star projections with representativeUpperBound of respective type parameter
 * to mimic behaviour of old FE (see StarProjectionImpl.getType())
 */
fun IrSimpleType.argumentTypesOrUpperBounds(): List<IrType> {
    val params = this.classOrUpperBound()!!.owner.typeParameters
    return arguments.mapIndexed { index, argument ->
        argument.typeOrNull ?: params[index].representativeUpperBound
    }
}

internal inline fun IrClass.shouldHaveSpecificSyntheticMethods(functionPresenceChecker: () -> IrSimpleFunction?) =
        !isSingleFieldValueClass && (isAbstractOrSealedSerializableClass || functionPresenceChecker() != null)

/**
 * This function checks if a deserialized property declares default value and has backing field.
 *
 * Returns (declaresDefaultValue, hasBackingField) boolean pair. Returns (false, false) for properties from current module.
 */
@OptIn(ObsoleteDescriptorBasedAPI::class)
fun IrProperty.analyzeIfFromAnotherModule(): Pair<Boolean, Boolean> {
    return if (descriptor is DeserializedPropertyDescriptor) { // IrLazyProperty does not deserialize backing fields correctly, so we should fall back to info from descriptor.
        // DeserializedPropertyDescriptor can be encountered only after K1, so it is safe to check it.
        val hasDefault = descriptor.declaresDefaultValue()
        hasDefault to (descriptor.backingField != null || hasDefault)
    } else if (this is Fir2IrLazyProperty) { // Fir2IrLazyProperty after K2 correctly deserializes information about backing field.
        // However, nor Fir2IrLazyProp nor deserialized FirProperty do not store default value (initializer expression) for property,
        // so we either should find corresponding constructor parameter and check its default, or rely on less strict check for default getter.
        // Comments are copied from PropertyDescriptor.declaresDefaultValue() as it has similar logic.
        val hasBackingField = backingField != null
        val matchingPrimaryConstructorParam = containingClass?.declarations?.filterIsInstance<FirPrimaryConstructor>()
            ?.singleOrNull()?.valueParameters?.find { it.name == this.name }
        if (matchingPrimaryConstructorParam != null) { // If property is a constructor parameter, check parameter default value
            // (serializable classes always have parameters-as-properties, so no name clash here)
            (matchingPrimaryConstructorParam.defaultValue != null) to hasBackingField
        } else { // If it is a body property, then it is likely to have initializer when getter is not specified
            // note this approach is not working well if we have smth like `get() = field`, but such cases on cross-module boundaries
            // should be very marginal. If we want to solve them, we need to add protobuf metadata extension.
            (fir.getter is FirDefaultPropertyGetter) to hasBackingField
        }
    } else {
        false to false
    }
}

/**
 * typeReplacement should be populated from FakeOverrides and is used when we want to determine the type for property
 * accounting for generic substitutions performed in subclasses:
 *
 * ```
 *    @Serializable
 *    sealed class TypedSealedClass<T>(val a: T) {
 *        @Serializable
 *        data class Child(val y: Int) : TypedSealedClass<String>("10")
 *     }
 * ```
 * In this case, serializableProperties for TypedSealedClass is a listOf(IrSerProp(val a: T)),
 * but for Child is a listOf(IrSerProp(val a: String), IrSerProp(val y: Int)).
 *
 * Using this approach, we can correctly deserialize parent's properties in Child.Companion.deserialize()
 */
@OptIn(ObsoleteDescriptorBasedAPI::class)
internal fun serializablePropertiesForIrBackend(irClass: IrClass,
        serializationDescriptorSerializer: SerializationDescriptorSerializerPlugin? = null,
        typeReplacement: Map<IrProperty, IrSimpleType>? = null): IrSerializableProperties {
    val properties = irClass.properties.toList()
    val primaryConstructorParams = irClass.primaryConstructor?.valueParameters.orEmpty()
    val primaryParamsAsProps = properties.associateBy { it.name }.let { namesMap ->
        primaryConstructorParams.mapNotNull {
            if (it.name !in namesMap) null else namesMap.getValue(it.name) to it.hasDefaultValue()
        }.toMap()
    }

    fun isPropSerializable(it: IrProperty) = if (irClass.isInternalSerializable) !it.annotations.hasAnnotation(
            SerializationAnnotations.serialTransientFqName)
    else !DescriptorVisibilities.isPrivate(it.visibility) && ((it.isVar && !it.annotations.hasAnnotation(
            SerializationAnnotations.serialTransientFqName)) || primaryParamsAsProps.contains(
            it)) && it.getter?.returnType != null // For some reason, some properties from Java (like java.net.URL.hostAddress) do not have getter. Let's ignore them, as they never have worked properly in K1 either.

    val (primaryCtorSerializableProps, bodySerializableProps) = properties.asSequence()
        .filter { !it.isFakeOverride && !it.isDelegated && it.origin != IrDeclarationOrigin.DELEGATED_MEMBER }
        .filter(::isPropSerializable).map {
            val isConstructorParameterWithDefault = primaryParamsAsProps[it] ?: false
            val (isPropertyFromAnotherModuleDeclaresDefaultValue, isPropertyWithBackingFieldFromAnotherModule) = it.analyzeIfFromAnotherModule()
            IrSerializableProperty(it, isConstructorParameterWithDefault,
                    it.backingField != null || isPropertyWithBackingFieldFromAnotherModule,
                    it.backingField?.initializer.let { init -> init != null && !init.expression.isInitializePropertyFromParameter() } || isConstructorParameterWithDefault || isPropertyFromAnotherModuleDeclaresDefaultValue,
                    typeReplacement?.get(it) ?: it.getter!!.returnType as IrSimpleType)
        }.filterNot { it.transient }.partition { primaryParamsAsProps.contains(it.ir) }

    var serializableProps = run {
        val supers = irClass.getSuperClassNotAny()
        if (supers == null || !supers.isInternalSerializable) {
            primaryCtorSerializableProps + bodySerializableProps
        } else {
            val originalToTypeFromFO = typeReplacement ?: buildMap<IrProperty, IrSimpleType> {
                irClass.properties.filter { it.isFakeOverride }.forEach { prop ->
                    val orig = prop.resolveFakeOverride()
                    val type = prop.getter?.returnType as? IrSimpleType
                    if (orig != null && type != null) put(orig, type)
                }
            }
            serializablePropertiesForIrBackend(supers, serializationDescriptorSerializer,
                    originalToTypeFromFO).serializableProperties + primaryCtorSerializableProps + bodySerializableProps
        }
    }

    serializableProps = restoreCorrectOrderFromClassProtoExtension(irClass.descriptor, serializableProps)

    val isExternallySerializable =
            irClass.isInternallySerializableEnum() || primaryConstructorParams.size == primaryParamsAsProps.size

    return IrSerializableProperties(serializableProps, isExternallySerializable, primaryCtorSerializableProps,
            bodySerializableProps)
}

fun BaseIrGenerator.getIrSerialTypeInfo(property: IrSerializableProperty,
        ctx: SerializationBaseContext): IrSerialTypeInfo {
    fun SerializableInfo(serializer: IrClassSymbol?) =
            IrSerialTypeInfo(property, if (property.type.isNullable()) "Nullable" else "", serializer)

    val T = property.type
    property.serializableWith(ctx)?.let { return SerializableInfo(it) }
    findAddOnSerializer(T, ctx)?.let { return SerializableInfo(it) }
    T.overriddenSerializer?.let { return SerializableInfo(it) }
    return when {
        T.isTypeParameter() -> IrSerialTypeInfo(property, if (property.type.isMarkedNullable()) "Nullable" else "",
                null)
        T.isPrimitiveType() -> IrSerialTypeInfo(property, T.classFqName!!.asString().removePrefix("kotlin."))
        T.isString() -> IrSerialTypeInfo(property, "String")
        T.isArray() -> {
            val serializer = property.serializableWith(ctx) ?: ctx.getClassFromInternalSerializationPackage(
                    SpecialBuiltins.referenceArraySerializer)
            SerializableInfo(serializer)
        }
        else -> {
            val serializer = findTypeSerializerOrContext(ctx, property.type)
            SerializableInfo(serializer)
        }
    }
}

fun BaseIrGenerator.findAddOnSerializer(propertyType: IrType, ctx: SerializationBaseContext): IrClassSymbol? {
    val classSymbol = propertyType.classOrNull ?: return null
    additionalSerializersInScopeOfCurrentFile[classSymbol to propertyType.isNullable()]?.let { return it }
    if (classSymbol in contextualKClassListInCurrentFile) return ctx.getClassFromRuntime(
            SpecialBuiltins.contextSerializer)
    if (classSymbol.owner.annotations.hasAnnotation(
                    SerializationAnnotations.polymorphicFqName)) return ctx.getClassFromRuntime(
            SpecialBuiltins.polymorphicSerializer)
    if (propertyType.isNullable()) return findAddOnSerializer(propertyType.makeNotNull(), ctx)
    return null
}

fun BaseIrGenerator?.findTypeSerializerOrContext(context: SerializationBaseContext, kType: IrType): IrClassSymbol? {
    if (kType.isTypeParameter()) return null
    return findTypeSerializerOrContextUnchecked(context, kType) ?: error(
            "Serializer for element of type ${kType.render()} has not been found")
}

fun BaseIrGenerator?.findTypeSerializerOrContextUnchecked(context: SerializationBaseContext,
        kType: IrType): IrClassSymbol? {
    val annotations = kType.annotations
    if (kType.isTypeParameter()) return null
    annotations.serializableWith()?.let { return it }
    this?.additionalSerializersInScopeOfCurrentFile?.get(kType.classOrNull!! to kType.isNullable())?.let {
        return it
    }
    if (kType.isMarkedNullable()) return findTypeSerializerOrContextUnchecked(context, kType.makeNotNull())
    if (this?.contextualKClassListInCurrentFile?.contains(kType.classOrNull) == true) return context.referenceClassId(
            contextSerializerId)
    return analyzeSpecialSerializers(context, annotations) ?: findTypeSerializer(context, kType)
}

fun analyzeSpecialSerializers(context: SerializationBaseContext, annotations: List<IrConstructorCall>): IrClassSymbol? =
        when {
            annotations.hasAnnotation(SerializationAnnotations.contextualFqName) || annotations.hasAnnotation(
                    SerializationAnnotations.contextualOnPropertyFqName) -> context.referenceClassId(
                    contextSerializerId) // can be annotation on type usage, e.g. List<@Polymorphic Any>
            annotations.hasAnnotation(SerializationAnnotations.polymorphicFqName) -> context.referenceClassId(
                    polymorphicSerializerId)
            else -> null
        }

fun findTypeSerializer(context: SerializationBaseContext, type: IrType): IrClassSymbol? {
    type.overriddenSerializer?.let { return it }
    if (type.isTypeParameter()) return null
    if (type.isArray()) return context.referenceClassId(referenceArraySerializerId)
    if (type.isGeneratedSerializableObject()) return context.referenceClassId(objectSerializerId)
    val stdSer = findStandardKotlinTypeSerializer(context, type) // see if there is a standard serializer
            ?: findEnumTypeSerializer(context, type)
    if (stdSer != null) return stdSer
    if (type.isInterface() && type.classOrNull?.owner?.isSealedSerializableInterface == false) return context.referenceClassId(
            polymorphicSerializerId)
    return type.classOrNull?.owner.classSerializer(context) // check for serializer defined on the type
}

fun findEnumTypeSerializer(context: SerializationBaseContext, type: IrType): IrClassSymbol? {
    val classSymbol = type.classOrNull?.owner ?: return null

    // in any case, the function returns the serializer for the enum
    if (classSymbol.kind != ClassKind.ENUM_CLASS) return null

    val legacySerializer =
            classSymbol.findEnumLegacySerializer() // $serializer for legacy compiled enums, or EnumSerializer for factories
    return legacySerializer?.symbol ?: context.referenceClassId(enumSerializerId)
}

internal fun IrClass?.classSerializer(context: SerializationBaseContext): IrClassSymbol? =
        this?.let { // serializer annotation on class?
            serializableWith?.let { return it } // companion object serializer?
            if (hasCompanionObjectAsSerializer) return companionObject()?.symbol // can infer @Poly?
            polymorphicSerializerIfApplicableAutomatically(context)?.let { return it } // default serializable?
            if (shouldHaveGeneratedSerializer()) { // $serializer nested class
                return this.declarations.filterIsInstance<IrClass>()
                    .singleOrNull { it.name == SerialEntityNames.SERIALIZER_CLASS_NAME }?.symbol
            }
            return null
        }

internal fun IrClass.polymorphicSerializerIfApplicableAutomatically(context: SerializationBaseContext): IrClassSymbol? {
    val serializer = when {
        kind == ClassKind.INTERFACE && modality == Modality.SEALED -> SpecialBuiltins.sealedSerializer
        kind == ClassKind.INTERFACE -> SpecialBuiltins.polymorphicSerializer
        isInternalSerializable && modality == Modality.ABSTRACT -> SpecialBuiltins.polymorphicSerializer
        isInternalSerializable && modality == Modality.SEALED -> SpecialBuiltins.sealedSerializer
        else -> null
    }
    return serializer?.let {
        context.getClassFromRuntimeOrNull(it, SerializationPackages.packageFqName,
                SerializationPackages.internalPackageFqName)
    }
}

fun findStandardKotlinTypeSerializer(context: SerializationBaseContext, type: IrType): IrClassSymbol? {
    val typeName = type.classFqName?.toString()
    val name = when (typeName) {
        "Z" -> if (type.isBoolean()) "BooleanSerializer" else null
        "B" -> if (type.isByte()) "ByteSerializer" else null
        "S" -> if (type.isShort()) "ShortSerializer" else null
        "I" -> if (type.isInt()) "IntSerializer" else null
        "J" -> if (type.isLong()) "LongSerializer" else null
        "F" -> if (type.isFloat()) "FloatSerializer" else null
        "D" -> if (type.isDouble()) "DoubleSerializer" else null
        "C" -> if (type.isChar()) "CharSerializer" else null
        null -> null
        else -> findStandardKotlinTypeSerializerName(typeName)
    } ?: return null
    return context.getClassFromRuntimeOrNull(name, SerializationPackages.internalPackageFqName,
            SerializationPackages.packageFqName)
}

// @Serializable(X::class) -> X
internal fun List<IrConstructorCall>.serializableWith(): IrClassSymbol? {
    val annotation = findAnnotation(SerializationAnnotations.serializableAnnotationFqName) ?: return null
    val arg = annotation.getValueArgument(0) as? IrClassReference ?: return null
    return arg.symbol as? IrClassSymbol
}

internal fun getSerializableClassByCompanion(companionClass: IrClass): IrClass? {
    if (companionClass.isSerializableObject) return companionClass
    if (!companionClass.isCompanion) return null
    val classDescriptor = (companionClass.parent as? IrClass) ?: return null
    if (!classDescriptor.shouldHaveGeneratedMethodsInCompanion) return null
    return classDescriptor
}

fun BaseIrGenerator?.allSealedSerializableSubclassesFor(irClass: IrClass,
        context: SerializationBaseContext): Pair<List<IrSimpleType>, List<IrClassSymbol>> {
    assert(irClass.modality == Modality.SEALED)
    fun recursiveSealed(klass: IrClass): Collection<IrClass> {
        return klass.sealedSubclasses.map { it.owner }
            .flatMap { if (it.modality == Modality.SEALED) recursiveSealed(it) else setOf(it) }
    }

    val serializableSubtypes = recursiveSealed(irClass).map { it.defaultType }
    return serializableSubtypes.mapNotNull { subtype ->
        findTypeSerializerOrContextUnchecked(context, subtype)?.let { Pair(subtype, it) }
    }.unzip()
}

internal fun SerializationBaseContext.getSerializableClassDescriptorBySerializer(serializer: IrClass): IrClass? {
    val serializerForClass = serializer.serializerForClass
    if (serializerForClass != null) return serializerForClass.owner
    if (serializer.name !in setOf(SerialEntityNames.SERIALIZER_CLASS_NAME,
                    SerialEntityNames.GENERATED_SERIALIZER_CLASS)) return null
    val classDescriptor = (serializer.parent as? IrClass) ?: return null
    if (!classDescriptor.shouldHaveGeneratedSerializer()) return null
    return classDescriptor
}

fun SerializationBaseContext.getClassFromRuntimeOrNull(className: String, vararg packages: FqName): IrClassSymbol? {
    val listToSearch = if (packages.isEmpty()) SerializationPackages.allPublicPackages else packages.toList()
    for (pkg in listToSearch) {
        referenceClassId(ClassId(pkg, Name.identifier(className)))?.let { return it }
    }
    return null
}

fun SerializationBaseContext.getClassFromRuntime(className: String, vararg packages: FqName): IrClassSymbol {
    return getClassFromRuntimeOrNull(className, *packages) ?: error("Class $className wasn't found in ${
        packages.toList().ifEmpty { SerializationPackages.allPublicPackages }
    }. " + "Check that you have correct version of serialization runtime in classpath.")
}

fun SerializationBaseContext.getClassFromInternalSerializationPackage(className: String): IrClassSymbol =
        getClassFromRuntimeOrNull(className, SerializationPackages.internalPackageFqName) ?: error(
                "Class $className wasn't found in ${SerializationPackages.internalPackageFqName}. Check that you have correct version of serialization runtime in classpath.")
