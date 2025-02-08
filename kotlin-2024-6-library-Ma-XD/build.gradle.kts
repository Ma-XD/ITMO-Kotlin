plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("org.jlleitschuh.gradle.ktlint") version "11.6.0"
}

group = "ru.itmo.ct"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://s01.oss.sonatype.org/content/repositories/releases/")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("io.github.pdvrieze.xmlutil:core:0.90.3")
    implementation("io.github.pdvrieze.xmlutil:serialization-jvm:0.90.3")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

ktlint {
    version = "0.50.0"
}
