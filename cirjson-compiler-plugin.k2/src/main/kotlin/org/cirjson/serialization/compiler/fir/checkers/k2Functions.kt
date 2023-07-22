package org.cirjson.serialization.compiler.fir.checkers

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.name.StandardClassIds

// ---------------------- annotation utils ----------------------

context(CheckerContext)
internal fun FirClassSymbol<*>.metaSerializableAnnotation(needArguments: Boolean): FirAnnotation? {
    val annotations = if (needArguments) resolvedAnnotationsWithClassIds else resolvedAnnotationsWithArguments
    return annotations.firstOrNull { it.isMetaSerializableAnnotation }
}

// ---------------------- class utils ----------------------

fun FirClassSymbol<*>.getSuperClassNotAny(session: FirSession): FirRegularClassSymbol? {
    return getSuperClassOrAny(session).takeUnless { it.classId == StandardClassIds.Any }
}

fun FirClassSymbol<*>.getSuperClassOrAny(session: FirSession): FirRegularClassSymbol {
    return resolvedSuperTypes.firstNotNullOfOrNull { superType ->
        superType.fullyExpandedType(session).toRegularClassSymbol(session)?.takeIf { it.classKind == ClassKind.CLASS }
    } ?: session.builtinTypes.anyType.toRegularClassSymbol(session) ?: error("Symbol for kotlin/Any not found")
}