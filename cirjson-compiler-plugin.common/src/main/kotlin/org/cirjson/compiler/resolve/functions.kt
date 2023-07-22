package org.cirjson.compiler.resolve

fun List<ISerializableProperty>.bitMaskSlotCount(): Int = size / 32 + 1

fun bitMaskSlotAt(propertyIndex: Int): Int = propertyIndex / 32

fun findStandardKotlinTypeSerializerName(typeName: String?): String? {
    return when (typeName) {
        null -> null
        "kotlin.Unit" -> "UnitSerializer"
        "kotlin.Nothing" -> "NothingSerializer"
        "kotlin.Boolean" -> "BooleanSerializer"
        "kotlin.Byte" -> "ByteSerializer"
        "kotlin.Short" -> "ShortSerializer"
        "kotlin.Int" -> "IntSerializer"
        "kotlin.Long" -> "LongSerializer"
        "kotlin.Float" -> "FloatSerializer"
        "kotlin.Double" -> "DoubleSerializer"
        "kotlin.Char" -> "CharSerializer"
        "kotlin.UInt" -> "UIntSerializer"
        "kotlin.ULong" -> "ULongSerializer"
        "kotlin.UByte" -> "UByteSerializer"
        "kotlin.UShort" -> "UShortSerializer"
        "kotlin.String" -> "StringSerializer"
        "kotlin.Pair" -> "CircularPairSerializer"
        "kotlin.Triple" -> "CircularTripleSerializer"
        "kotlin.collections.Collection", "kotlin.collections.List", "kotlin.collections.ArrayList", "kotlin.collections.MutableList" -> "CircularArrayListSerializer"
        "kotlin.collections.Set", "kotlin.collections.LinkedHashSet", "kotlin.collections.MutableSet" -> "CircularLinkedHashSetSerializer"
        "kotlin.collections.HashSet" -> "CircularHashSetSerializer"
        "kotlin.collections.Map", "kotlin.collections.LinkedHashMap", "kotlin.collections.MutableMap" -> "CircularLinkedHashMapSerializer"
        "kotlin.collections.HashMap" -> "CircularHashMapSerializer"
        "kotlin.collections.Map.Entry" -> "CircularMapEntrySerializer"
        "kotlin.ByteArray" -> "CircularByteArraySerializer"
        "kotlin.ShortArray" -> "CircularShortArraySerializer"
        "kotlin.IntArray" -> "CircularIntArraySerializer"
        "kotlin.LongArray" -> "CircularLongArraySerializer"
        "kotlin.UByteArray" -> "CircularUByteArraySerializer"
        "kotlin.UShortArray" -> "CircularUShortArraySerializer"
        "kotlin.UIntArray" -> "CircularUIntArraySerializer"
        "kotlin.ULongArray" -> "CircularULongArraySerializer"
        "kotlin.CharArray" -> "CircularCharArraySerializer"
        "kotlin.FloatArray" -> "CircularFloatArraySerializer"
        "kotlin.DoubleArray" -> "CircularDoubleArraySerializer"
        "kotlin.BooleanArray" -> "CircularBooleanArraySerializer"
        "kotlin.time.Duration" -> "CircularDurationSerializer"
        "java.lang.Boolean" -> "BooleanSerializer"
        "java.lang.Byte" -> "ByteSerializer"
        "java.lang.Short" -> "ShortSerializer"
        "java.lang.Integer" -> "IntSerializer"
        "java.lang.Long" -> "LongSerializer"
        "java.lang.Float" -> "FloatSerializer"
        "java.lang.Double" -> "DoubleSerializer"
        "java.lang.Character" -> "CharSerializer"
        "java.lang.String" -> "StringSerializer"
        "java.util.Collection", "java.util.List", "java.util.ArrayList" -> "CircularArrayListSerializer"
        "java.util.Set", "java.util.LinkedHashSet" -> "CircularLinkedHashSetSerializer"
        "java.util.HashSet" -> "CircularHashSetSerializer"
        "java.util.Map", "java.util.LinkedHashMap" -> "CircularLinkedHashMapSerializer"
        "java.util.HashMap" -> "CircularHashMapSerializer"
        "java.util.Map.Entry" -> "CircularMapEntrySerializer"
        else -> return null
    }
}
