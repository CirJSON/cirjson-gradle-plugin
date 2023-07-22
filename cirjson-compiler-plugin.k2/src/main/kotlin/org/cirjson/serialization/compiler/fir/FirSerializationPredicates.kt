package org.cirjson.serialization.compiler.fir

import org.cirjson.compiler.resolve.SerializationAnnotations
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate

object FirSerializationPredicates {

    internal val serializerFor = DeclarationPredicate.create {
        annotated(setOf(SerializationAnnotations.serializerAnnotationFqName)) // @Serializer(for=...)
    }

    internal val hasMetaAnnotation = DeclarationPredicate.create {
        metaAnnotated(SerializationAnnotations.metaSerializableAnnotationFqName, includeItself = false)
    }

    internal val annotatedWithSerializableOrMeta = DeclarationPredicate.create {
        annotated(setOf(SerializationAnnotations.serializableAnnotationFqName)) or metaAnnotated(
                SerializationAnnotations.metaSerializableAnnotationFqName, includeItself = false)
    }

}