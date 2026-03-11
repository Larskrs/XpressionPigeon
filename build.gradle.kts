plugins {
    java
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.serialization") version "2.1.20"
    id("io.ktor.plugin") version "3.1.2"
    id("com.gradleup.shadow") version "9.0.0"
}

sourceSets.main {
    resources {
        srcDir(file("."))
        include("frontend/dist/**")
    }
}

group = "com.example"
version = "0.0.2"

val ktor_version = "3.1.2"

application {
    mainClass.set("com.example.MainKt")
}

tasks.withType<Jar> {
    manifest {
        attributes("Implementation-Version" to project.version)
    }
}

tasks.named<JavaExec>("run") {
    environment("DEV_MODE", "true")
    systemProperty("io.ktor.development", "true")
    systemProperty("frida.version", version as String)
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

dependencies {
    // Ktor Server
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-server-websockets:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-server-config-yaml:$ktor_version")

    // Serialization
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-serialization-gson:$ktor_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Ktor Client
    implementation("io.ktor:ktor-client-cio:$ktor_version")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")

    // YAML
    implementation("com.charleskorn.kaml:kaml:0.54.0")

    // Exposed ORM
    implementation("org.jetbrains.exposed:exposed-core:0.47.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.47.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.47.0")
    implementation("org.jetbrains.exposed:exposed-json:0.47.0")

    // Database
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("com.zaxxer:HikariCP:5.1.0")

    // Test
    testImplementation("io.ktor:ktor-server-test-host:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.22")
}