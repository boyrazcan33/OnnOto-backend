FROM maven:3.9-eclipse-temurin-21 as build
WORKDIR /app
COPY onnoto-backend/ ./
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Add JVM memory limits
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70.0 -Xms128m -Xmx512m -XX:+UseG1GC"

# Add the start script
RUN echo '#!/bin/sh\n\
echo "Starting application with memory settings: $JAVA_OPTS"\n\
\n\
# Run the application with memory limits\n\
exec java $JAVA_OPTS -jar app.jar\n\
' > /app/start.sh

RUN chmod +x /app/start.sh

# Use the startup script
CMD ["/app/start.sh"]