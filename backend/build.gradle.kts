plugins {
    java
    application
    id("com.gradleup.shadow") version "8.3.6"
}

group = "com.charmed"
version = "0.1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "com.charmed.App"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.11.0")
}

tasks.shadowJar {
    archiveBaseName = "backend"
    archiveClassifier = "all"
    archiveVersion = ""
    mergeServiceFiles()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("--enable-preview"))
}

tasks.withType<JavaExec> {
    jvmArgs("--enable-preview")
}
