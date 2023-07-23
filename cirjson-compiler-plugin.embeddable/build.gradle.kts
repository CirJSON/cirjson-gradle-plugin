plugins {
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    compileOnly(project(":cirjson-compiler-plugin.common"))
    compileOnly(project(":cirjson-compiler-plugin.k1"))
    compileOnly(project(":cirjson-compiler-plugin.k2"))
    compileOnly(project(":cirjson-compiler-plugin.backend"))
    compileOnly(project(":cirjson-compiler-plugin.cli"))
}

tasks.named<Jar>("jar") {
    val subPlugins = listOf("common", "k1", "k2", "backend", "cli")
    val pluginRegex = Regex("cirjson-compiler-plugin\\.(?:${subPlugins.reduce { acc, s -> "$acc|$s" }})-${project.version}\\.jar")

    from(configurations["compileClasspath"].filter {
        pluginRegex.matches(it.name)
    }.map {
        if (it.isDirectory) it else zipTree(it)
    })
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
