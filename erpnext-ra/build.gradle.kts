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
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
}

tasks.jar {
    archiveBaseName.set("erpnext-ra")
}

val rar by tasks.registering(Zip::class) {
    group = "build"
    description = "Packages the ERPNext resource adapter as a .rar archive for WildFly"

    dependsOn(tasks.jar)

    archiveBaseName.set("erpnext-ra")
    archiveVersion.set(project.version.toString())
    archiveExtension.set("rar")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))

    from("src/main/resources") {
        include("META-INF/ra.xml")
        include("META-INF/ironjacamar.xml")
    }
    from(tasks.jar)
}

tasks.assemble {
    dependsOn(rar)
}
