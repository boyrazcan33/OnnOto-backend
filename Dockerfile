FROM openjdk:21-slim

WORKDIR /app

# Copy the JAR file from onnoto-backend/target
COPY onnoto-backend/target/onnoto-backend-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8087

# Run the application
CMD ["java", "-jar", "app.jar"]