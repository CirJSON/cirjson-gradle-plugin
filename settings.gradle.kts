pluginManagement { //    includeBuild("repo/gradle-settings-conventions")
    //    includeBuild("repo/gradle-build-conventions")

    repositories {
        maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins { //    id("build-cache")
    //    id("gradle-enterprise")
    //    id("jvm-toolchain-provisioning")
    //    id("kotlin-daemon-config")
}

rootProject.name = "cirjson-gradle-plugin"

include(":cirjson-compiler-plugin.common")
include(":cirjson-compiler-plugin.k1")
include(":cirjson-compiler-plugin.k2")
include(":cirjson-compiler-plugin.backend")

