plugins {
    kotlin("jvm") version "1.9.22"
    java
}

group = "de.c4vxl"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.dv8tion:JDA:5.0.0-beta.21")
    implementation("org.json:json:20231013")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "$group.MainKt"
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().filter { it.name != "module-info.class" }.map { if (it.isDirectory) it else zipTree(it) })
}

kotlin {
    jvmToolchain(17)
}