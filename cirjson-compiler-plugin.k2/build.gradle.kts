import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-compiler:1.9.0")
    implementation(project(":cirjson-compiler-plugin.common"))
}

tasks.withType(KotlinCompile::class).all {
    kotlinOptions {
        freeCompilerArgs += "-Xcontext-receivers"
    }
}
