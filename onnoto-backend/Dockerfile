FROM openjdk:21-slim

WORKDIR /app

# Copy JAR file
COPY target/onnoto-backend-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8087

# Run with environment variables supplied at runtime
ENTRYPOINT ["java", "-jar", "app.jar"]