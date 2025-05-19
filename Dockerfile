FROM maven:3.9-openjdk-21 as build
WORKDIR /app
COPY onnoto-backend/ ./
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

FROM openjdk:21
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
CMD ["sh", "-c", "java -jar app.jar --server.port=${PORT:8087}"]