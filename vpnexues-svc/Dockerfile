# Multi-stage Dockerfile for vpnexues-svc (Render Docker runtime)
# Place at: vpnreactsource/vpnexues-svc/Dockerfile
# Build context must be vpnexues-svc/ (where gradlew lives).

# ---------- build ----------
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Wrapper + build files first (better layer cache)
COPY gradlew gradlew.bat build.gradle settings.gradle ./
COPY gradle/ gradle/
RUN chmod +x gradlew

# Source
COPY src/ src/

# Skip tests on deploy image (CI can run them separately)
RUN ./gradlew bootJar --no-daemon -x test

# ---------- run ----------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Non-root
RUN useradd -r -u 1001 vpnexues
USER vpnexues

COPY --from=build /app/build/libs/vpnexues-svc-0.0.1-SNAPSHOT.jar app.jar

# Render injects PORT (default 10000 on free web services)
ENV JAVA_OPTS=""
EXPOSE 10000

# Use PORT if present so Render health checks pass
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-10000} -jar app.jar"]
