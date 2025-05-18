FROM openjdk:21-slim

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy JAR file
COPY target/onnoto-backend-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8087

# Run with environment variables supplied at runtime
ENTRYPOINT ["java", "-jar", "app.jar"]