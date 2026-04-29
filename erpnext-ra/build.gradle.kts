plugins {
    `java-library`
}

group = "ru.chousik"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("jakarta.resource:jakarta.resource-api:2.1.0")
}

tasks.jar {
    archiveBaseName.set("erpnext-ra")
}
