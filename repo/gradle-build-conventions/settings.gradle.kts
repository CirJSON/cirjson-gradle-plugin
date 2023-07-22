import java.util.*

pluginManagement {
    apply(from = "../scripts/cache-redirector.settings.gradle.kts")
    apply(from = "../scripts/kotlin-bootstrap.settings.gradle.kts")

    includeBuild("../gradle-settings-conventions")

    repositories {
        maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies") }
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

plugins {
    id("build-cache")
    id("gradle-enterprise")
    id("jvm-toolchain-provisioning")
    id("kotlin-daemon-config")
}

val versionPropertiesFile = File(rootProject.projectDir.parentFile, "../gradle/versions.properties")
val versionProperties = Properties()
versionProperties.load(versionPropertiesFile.inputStream())

dependencyResolutionManagement {
    components {
        withModule("com.google.code.gson:gson") {
            allVariants {
                withDependencies {
                    add("com.google.code.gson:gson") {
                        version {
                            require(versionProperties["versions.gson"] as String)
                        }
                        because("Force using same gson version because of https://github.com/google/gson/pull/1991")
                    }
                }
            }
        }
    }
}

include(":prepare-deps")