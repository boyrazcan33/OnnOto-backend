FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy JAR file
COPY target/onnoto-backend-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# Set entry point
ENTRYPOINT ["java", "-jar", "app.jar"]

# Set environment variables
ENV ONNOTO_DB_URL="jdbc:postgresql://your-prod-db-host:5432/onnoto"
ENV ONNOTO_DB_USERNAME="prod_username"
ENV ONNOTO_DB_PASSWORD="prod_password"
ENV ONNOTO_REDIS_PASSWORD="redis_password"