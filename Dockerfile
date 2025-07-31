# Multi-stage build
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /workspace

# Copy Gradle files
COPY gradlew ./
COPY gradle/ gradle/
COPY build.gradle.kts ./
COPY settings.gradle ./
COPY gradle.properties ./

# Copy source code
COPY src/ src/

# Build the application
RUN chmod +x gradlew && ./gradlew clean build -x test

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app

# Create a non-root user
RUN groupadd -r bitcoin && useradd -r -g bitcoin bitcoin

# Copy the jar file from builder stage
COPY --from=builder /workspace/build/libs/BitcoinMarketRecorder-0.0.1-SNAPSHOT.jar ./app.jar

# Create directories and set permissions
RUN mkdir -p /app/csv && chown -R bitcoin:bitcoin /app

# Switch to non-root user
USER bitcoin

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]