plugins {
    kotlin("jvm")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-compiler:1.9.0")
    implementation(project(":cirjson-compiler-plugin.common"))
}