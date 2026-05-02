FROM gradle:8.14.3-jdk21 AS builder

WORKDIR /workspace

COPY gradle gradle
COPY gradlew settings.gradle.kts build.gradle.kts ./
RUN chmod +x gradlew

COPY src src

RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:21-jre

WORKDIR /app

ENV PORT=8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

RUN groupadd --system spring && useradd --system --gid spring spring

COPY --from=builder /workspace/build/libs/*.jar /app/app.jar

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
