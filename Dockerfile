FROM openjdk:21-slim

WORKDIR /app

# Copy the entire backend directory
COPY onnoto-backend/ /app/

# Make the Maven wrapper executable and build the application
RUN chmod +x /app/mvnw && \
    cd /app && \
    ./mvnw clean package -DskipTests

# Run the application
EXPOSE 8087
ENV SERVER_PORT=8087

CMD ["java", "-jar", "/app/target/onnoto-backend-0.0.1-SNAPSHOT.jar"]