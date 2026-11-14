# Build stage
FROM eclipse-temurin:27-jdk AS build
WORKDIR /workspace

# Download dependencies first so Docker caches them until pom.xml changes
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

# Build the jar (tests included) and split it into layers
COPY src src
RUN ./mvnw -B package \
    && java -Djarmode=tools -jar target/astrasoil-back-1.0.0.jar extract --layers --launcher --destination target/extracted

# Runtime stage
FROM eclipse-temurin:27-jre
WORKDIR /app
# data/ holds the plants copy the app writes at runtime
RUN groupadd --system spring && useradd --system --gid spring spring \
    && mkdir -p /app/data && chown spring:spring /app/data
USER spring

# Spring Boot layers, from least to most often changed
COPY --from=build /workspace/target/extracted/dependencies/ ./
COPY --from=build /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/target/extracted/application/ ./

EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
