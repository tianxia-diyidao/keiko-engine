// keiko-engine — content-agnostic flashcard study engine, Spring Boot edition.
// Java 21 LTS + Gradle Kotlin DSL + Spring Boot 3.4.x.
//
// One-command run:
//   Windows:   .\gradlew.bat bootRun
//              (or .\run.ps1 which wraps clean+test+bootRun)
//   Mac/Linux: ./gradlew bootRun
//              (or `make run`)
//
// One-command deploy to Fly.io:  fly deploy

plugins {
    java
    id("org.springframework.boot") version "3.4.5"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "io.tenka"
version = "v0.3"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot core: web (embedded Tomcat) + Thymeleaf templates.
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    // Validation for DTO records.
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // TOML parsing for subject manifests. We use Jackson's TOML module
    // since Spring Boot already brings Jackson — one fewer dependency to
    // audit.
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-toml")

    // Test stack: JUnit 5 + AssertJ + MockMvc come bundled in starter-test.
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

// Spring Boot's bootJar produces a single fat JAR that runs anywhere
// with a JVM. Size after `./gradlew bootJar` is ~30 MB; that's what the
// Dockerfile copies into the runtime image.
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("keiko-engine.jar")
}
