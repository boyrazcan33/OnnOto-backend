FROM maven:3.9-eclipse-temurin-21 as build
WORKDIR /app
COPY onnoto-backend/ ./
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Add JVM memory settings and port configuration
CMD ["sh", "-c", "java -XX:MaxRAMPercentage=75.0 -Xms128m -jar app.jar --server.port=${PORT:-8087}"]