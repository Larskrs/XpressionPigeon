// rosstalk-mock/build.gradle.kts
plugins {
    kotlin("jvm") version "2.1.20"
    application
}

application {
    mainClass.set("com.example.mock.MainKt")
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}