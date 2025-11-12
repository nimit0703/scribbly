# Build stage
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
# Copy source code
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage  
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy the built JAR from builder stage
COPY --from=builder /app/target/game-0.0.1-SNAPSHOT.jar app.jar

# Create a non-root user for security
RUN groupadd -r spring && useradd -r -g spring spring
# USER spring

# Expose port
EXPOSE 8080 8443

# Health check (using the installed curl)
# HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
#     CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]