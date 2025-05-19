# Build stage
FROM maven:3.9-openjdk-21 as build
WORKDIR /app
COPY onnoto-backend/ ./
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# Runtime stage
FROM openjdk:21
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Add a script to set up PostgreSQL with PostGIS
RUN apt-get update && apt-get install -y postgresql-client

# Create a script to set up PostGIS before starting the app
RUN echo '#!/bin/sh \n\
# Try to create PostGIS extension (ignore errors) \n\
PGPASSWORD=$ONNOTO_DB_PASSWORD psql -h $PGHOST -p $PGPORT -U $PGUSER -d $PGDATABASE -c "CREATE EXTENSION IF NOT EXISTS postgis;" || true \n\
\n\
# Start the Java app \n\
java -jar app.jar --server.port=${PORT:8087} \n\
' > /app/start.sh

RUN chmod +x /app/start.sh

# Run the script when container starts
CMD ["/app/start.sh"]