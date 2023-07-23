plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    `maven-publish`
}

dependencies {
    implementation(kotlin("gradle-plugin-api"))
    implementation(project(":"))
}

gradlePlugin {
    plugins {
        create("cirJsonSerialization") {
            id = "org.cirjson.plugin.serialization"
            displayName = "Kotlin compiler plugin for kotlinx.serialization library"
            description = displayName
            implementationClass = "org.cirjson.serialization.gradle.SerializationGradleSubplugin"
        }
    }
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>(project.name) {
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "localPluginRepository"
            url = uri("../../local-plugin-repository")
        }
    }
}
