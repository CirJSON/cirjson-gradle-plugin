package org.cirjson.compiler.backend.common

import org.jetbrains.kotlin.descriptors.ClassDescriptor

val ClassDescriptor.isStaticSerializable: Boolean get() = this.declaredTypeParameters.isEmpty()
