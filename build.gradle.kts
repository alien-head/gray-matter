plugins {
    kotlin("jvm") version "2.1.10" apply false
    kotlin("plugin.serialization") version "2.1.10" apply false
    id("io.ktor.plugin") version "3.1.0" apply false
}


allprojects {
    repositories {
        mavenCentral()
    }
}
