package org.cirjson.compiler.extensions

enum class SerializationIntrinsicsState {

    NORMAL, // depends on whether we have noCompiledSerializer function in runtime

    DISABLED, // disabled if corresponding CLI flag passed

    FORCE_ENABLED // used for test purposes ONLY

}