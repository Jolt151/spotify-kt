plugins {
    kotlin("jvm") version "1.8.0"
    `maven-publish`
}

group = "io.github.tiefensuche"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.json:json:20240303")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(11)
}

publishing {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/0xf4b1/spotify-kt")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
    publications {
        register<MavenPublication>("gpr") {
            from(components["java"])
        }
    }
}