# Stage 1: Build the Spring Boot application
FROM maven:3.9-eclipse-temurin-21 as maven-build
WORKDIR /app
COPY onnoto-backend/ ./
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# Stage 2: Create runtime image with PostGIS
FROM postgres:16-bullseye

# Install PostGIS
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        postgresql-16-postgis-3 \
        postgresql-16-postgis-3-scripts \
    && rm -rf /var/lib/apt/lists/*

# Install JRE
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        openjdk-21-jre-headless \
    && rm -rf /var/lib/apt/lists/*

# Copy the Spring Boot application
WORKDIR /app
COPY --from=maven-build /app/target/*.jar /app/app.jar

# Copy init scripts
COPY --from=maven-build /app/target/classes/db/migration/ /docker-entrypoint-initdb.d/
RUN chmod -R +x /docker-entrypoint-initdb.d/

# Add script to initialize PostGIS and start both Postgres and the Java application
RUN echo '#!/bin/bash\n\
# Start PostgreSQL service in the background\n\
/usr/local/bin/docker-entrypoint.sh postgres &\n\
\n\
# Wait for PostgreSQL to start accepting connections\n\
until pg_isready -U postgres; do\n\
  echo "Waiting for PostgreSQL to start..."\n\
  sleep 1\n\
done\n\
\n\
# Create PostGIS extension\n\
psql -U postgres -d postgres -c "CREATE EXTENSION IF NOT EXISTS postgis;"\n\
\n\
# Start the Spring Boot application\n\
DATABASE_URL="jdbc:postgresql://localhost:5432/postgres"\n\
DATABASE_USERNAME="postgres"\n\
DATABASE_PASSWORD="postgres"\n\
\n\
# Set environment variables for the application\n\
export ONNOTO_DB_URL="$DATABASE_URL"\n\
export ONNOTO_DB_USERNAME="$DATABASE_USERNAME"\n\
export ONNOTO_DB_PASSWORD="$DATABASE_PASSWORD"\n\
\n\
# Start Spring Boot app with the PORT environment variable\n\
java -jar /app/app.jar --server.port=${PORT:-8087}\n\
' > /app/start.sh

RUN chmod +x /app/start.sh

# Expose ports for PostgreSQL and Spring Boot
EXPOSE 5432 8087

# Run both services
CMD ["/app/start.sh"]