import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute

buildscript { // a workaround for kotlin compiler classpath in kotlin project: sometimes gradle substitutes
    // kotlin-stdlib external dependency with local project :kotlin-stdlib in kotlinCompilerClasspath configuration.
    // see also configureCompilerClasspath@
    val bootstrapCompilerClasspath by configurations.creating

    dependencies {
        bootstrapCompilerClasspath(kotlin("compiler-embeddable", "1.9.20-dev-5788"))
        classpath("org.jetbrains.kotlin:kotlin-build-gradle-plugin:${"0.0.39"}")
    }

}

plugins {
    kotlin("jvm") version "1.9.0"
    `kotlin-dsl`
}

group = "org.cirjson.gradle.plugin"
version = "1.0-SNAPSHOT"

val jsonJsIrRuntimeForTests: Configuration by configurations.creating {
    attributes {
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
        attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
    }
}

val coreJsIrRuntimeForTests: Configuration by configurations.creating {
    attributes {
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
        attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
    }
}

subprojects {
    repositories {
        repositories {
            maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies") }
            google()
            mavenCentral()
            gradlePluginPortal()
        }
    }
}

repositories {
    mavenCentral()
}

dependencies { //    embedded(project(":kotlinx-serialization-compiler-plugin.common"))// { isTransitive = false }
    //    embedded(project(":kotlinx-serialization-compiler-plugin.k1")) { isTransitive = false }
    //    embedded(project(":kotlinx-serialization-compiler-plugin.k2")) { isTransitive = false }
    //    embedded(project(":kotlinx-serialization-compiler-plugin.backend")) { isTransitive = false }
    //    embedded(project(":kotlinx-serialization-compiler-plugin.cli")) { isTransitive = false }
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}