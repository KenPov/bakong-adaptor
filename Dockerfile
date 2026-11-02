# syntax=docker/dockerfile:1

# ---- Build stage -------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace

ARG SKIP_TESTS=false

# Resolve dependencies first so they are cached in their own layer and only
# re-downloaded when pom.xml actually changes.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

COPY src src
RUN --mount=type=cache,target=/root/.m2 \
    if [ "$SKIP_TESTS" = "true" ]; then \
        mvn -B -q -DskipTests package; \
    else \
        mvn -B -q package; \
    fi

# ---- Runtime stage -------------------------------------------------------------
FROM eclipse-temurin:25-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -r spring && useradd -r -g spring spring

WORKDIR /app
COPY --from=build /workspace/target/bakong-adaptor-*.jar app.jar
RUN mkdir -p /app/logs && chown -R spring:spring /app

USER spring:spring
EXPOSE 8080 8082

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:${SERVER_PORT:-8080}/actuator/health/liveness || exit 1

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS:-} -jar app.jar"]
