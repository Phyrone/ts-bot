import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.3.41"
    id("com.github.johnrengelman.shadow") version "4.0.4"
}

group = "de.phyrone"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    maven("https://jitpack.io")
    maven("https://libraries.minecraft.net")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    testCompile("junit", "junit", "4.12")
    compile("org.slf4j", "slf4j-simple", "1.7.21")

    compile("org.fusesource.jansi:jansi:1.18")
    compile("com.squareup.okhttp3", "okhttp", "4.0.+")
    compile("com.github.phyrone", "brigardier-kotlin", "1.3.+")

    compile("com.fasterxml.jackson.core", "jackson-databind", "2.17.+")
    compile("com.fasterxml.jackson.module", "jackson-module-kotlin", "2.9.+")

    compile("info.picocli:picocli:4.+")
    compile("org.beryx:text-io:3.3.+")

    compile("com.sedmelluq:lavaplayer:1.3.+")
    //compile("com.github.Manevolent:ts3j:1.0")
    compile("com.github.Phyrone:ts3j:0b5d63b6f7")

}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
val mainClassPath = "de.phyrone.tsbot.TsBotServer"
tasks.withType<Jar> {

    manifest {
        attributes(
            mapOf(
                "Main-Class" to mainClassPath
            )
        )
    }
}
tasks.withType<ShadowJar> {
    baseName = "mini-ts-bot"
    classifier = null
    version = null
    // minimize{ exclude("com.fasterxml.jackson.*:.*:.*", "org.jetbrains.kotlin:kotlin-reflect:.*") }

}
