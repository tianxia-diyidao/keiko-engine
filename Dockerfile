# Multi-stage Dockerfile for keiko-engine.
#
# Stage 1: build the fat JAR with Eclipse Temurin JDK 21.
# Stage 2: run on the slim JRE 21 image (~70 MB base + ~30 MB JAR).
#
# Fly.io reads this directly via `fly deploy` — no separate buildpack needed.

# ─── Stage 1: build ────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Cache Gradle dependencies in their own layer so source changes don't
# bust the dependency download cache.
COPY gradlew gradlew.bat ./
COPY gradle gradle
COPY settings.gradle.kts build.gradle.kts ./
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# Now copy sources and build the fat JAR.
COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# ─── Stage 2: runtime ──────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user for security best practice.
RUN addgroup -S keiko && adduser -S keiko -G keiko
USER keiko

COPY --from=build /workspace/build/libs/keiko-engine.jar app.jar

# Fly.io maps PORT env var to whatever it allocates; application.yml
# already reads ${PORT:8080}.
EXPOSE 8080

# JVM tuning for small VMs (Fly free tier 256MB):
#   - container-aware (cgroup) memory limits via UseContainerSupport (default in JDK 11+)
#   - tight heap so we don't get OOM-killed
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-jar", "app.jar"]
