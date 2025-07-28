# Use pre-built jar approach
FROM eclipse-temurin:17-jre
WORKDIR /app

# Create a non-root user
RUN groupadd -r bitcoin && useradd -r -g bitcoin bitcoin

# Copy the jar file (build locally first)
COPY build/libs/*.jar app.jar

# Create directories and set permissions
RUN mkdir -p /app/csv && chown -R bitcoin:bitcoin /app

# Switch to non-root user
USER bitcoin

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]