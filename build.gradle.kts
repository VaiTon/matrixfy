plugins {
    kotlin("jvm") version "1.8.20"
    kotlin("plugin.serialization") version "1.8.10"
    application
}

group = "io.matrixfy"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    val spotifyApiVersion = "4.0.0"
    implementation("com.adamratzman:spotify-api-kotlin-core:$spotifyApiVersion")

    val trixnityVersion = "3.5.1"
    implementation("net.folivo:trixnity-client:$trixnityVersion")
    implementation("net.folivo:trixnity-client-media-okio:$trixnityVersion")
    implementation("net.folivo:trixnity-client-repository-exposed:$trixnityVersion")
    implementation("net.folivo:trixnity-client-repository-realm:$trixnityVersion")

    val exposedVersion = "0.41.1"
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")

    val kotlinxHtmlVersion = "0.8.0"
    implementation("org.jetbrains.kotlinx:kotlinx-html:$kotlinxHtmlVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:$kotlinxHtmlVersion")

    implementation("io.ktor:ktor-client-java-jvm:2.2.4")
    implementation("ch.qos.logback:logback-classic:1.4.6")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("io.matrixfy.MainKt")
}

tasks.test {
    useJUnitPlatform()
}

tasks.compileKotlin {
    kotlinOptions {
        jvmTarget = "17"
    }
}