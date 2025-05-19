FROM maven:3.9-eclipse-temurin-21 as build
WORKDIR /app
COPY onnoto-backend/ ./
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

CMD java -jar app.jar