plugins {
    kotlin("jvm")
    `kotlin-dsl`
}

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0")
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-compiler:1.9.0")
}