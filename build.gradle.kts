plugins {
    id("java")
    id("application")
}

group = "org.UTS"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

application {
    // Ajusta si tu clase main está en otro paquete
    mainClass.set("com.example.asr.App")
}

dependencies {
    // Vosk - Speech Recognition Library
    implementation("com.alphacephei:vosk:0.3.45")

    // JNA (Java Native Access) - Requerido por Vosk
    implementation("net.java.dev.jna:jna:5.13.0")

    // JSON - Para parsear resultados de Vosk
    implementation("org.json:json:20230227")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.12")
    implementation("org.slf4j:slf4j-simple:2.0.12")

    // Testing
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}