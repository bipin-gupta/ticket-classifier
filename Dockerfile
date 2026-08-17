# ---------- Stage 1: build the jar with Maven ----------
# Official Maven image bundled with an Eclipse Temurin 21 JDK - has
# everything needed to compile the project and package it into a jar.
# This stage is only used to build; it is not part of the final image.
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy just the pom first and download dependencies. Docker caches this
# layer, so dependencies are only re-downloaded when pom.xml changes -
# not on every source code change.
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Now copy the actual source and build the jar (skip tests here to keep
# the image build fast; drop -DskipTests if you want tests to run too).
COPY src ./src
RUN mvn -B -DskipTests package

# ---------- Stage 2: run the jar with just a JRE ----------
# Much smaller than the build stage - no Maven, no compiler, just enough
# Java to run an already-built jar. This is the image that actually ships.
FROM eclipse-temurin:21-jre AS run

WORKDIR /app

# Run as a non-root user instead of root - standard container hardening.
RUN useradd --create-home --shell /bin/bash appuser

# Copy the jar built in the previous stage. The wildcard means this line
# doesn't need updating if the version in pom.xml changes.
COPY --from=build --chown=appuser:appuser /app/target/*.jar app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
