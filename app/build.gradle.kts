plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("io.ktor.plugin")
}

dependencies {
    val kotlinxSerialization: String by project
    val postgresql: String by project
    val logback: String by project
    val swagger: String by project

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerialization")

    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")

    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-cio")
    implementation("io.ktor:ktor-client-content-negotiation")
    implementation("io.ktor:ktor-server-openapi")

    implementation("io.swagger.codegen.v3:swagger-codegen-generators:$swagger")

    implementation("org.postgresql:postgresql:$postgresql")

    implementation("ch.qos.logback:logback-classic:$logback")

    implementation(project(":blockchain"))
    implementation(project(":crypto"))
    implementation(project(":network"))
    implementation(project(":storage"))
    implementation(project(":sql"))
}

application {
    mainClass.set("io.alienhead.gray.matter.app.AppKt")
}
