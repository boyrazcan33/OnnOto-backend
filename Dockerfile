FROM maven:3.9-eclipse-temurin-21 as build

WORKDIR /app

# Copy the entire onnoto-backend directory
COPY onnoto-backend/ ./

# Make Maven wrapper executable
RUN chmod +x mvnw

# Build the application
RUN ./mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built JAR file
COPY --from=build /app/target/*.jar app.jar

# Run the application
CMD ["java", "-jar", "app.jar"]