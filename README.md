# OnnOto-backend

# OnnOto - EV Charging Station Reliability Platform

<div align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.4.5-brightgreen" alt="Spring Boot Version"/>
  <img src="https://img.shields.io/badge/JDK-21-blue" alt="JDK Version"/>
  <img src="https://img.shields.io/badge/PostgreSQL-with%20PostGIS-blue" alt="PostgreSQL with PostGIS"/>
  <img src="https://img.shields.io/badge/Redis-Cache-red" alt="Redis"/>
  <img src="https://img.shields.io/badge/Deployment-Fly.io-purple" alt="Fly.io"/>
</div>

## 🔋 About OnnOto

OnnOto is a backend service for monitoring and analyzing the reliability of electric vehicle charging stations across Estonia. The platform helps EV drivers find reliable charging stations by collecting, analyzing, and presenting real-time data on station availability and performance.

## ✨ Key Features

- **Real-time Monitoring**: Track station and connector status
- **Reliability Metrics**: Quantify and rank station reliability
- **Anomaly Detection**: Identify problematic stations through pattern analysis
- **Geospatial Search**: Find nearby charging stations with PostGIS
- **Multi-network Integration**: Support for ELMO, Eleport, and other charging networks
- **Analytics Engine**: Historical data analysis and visualization endpoints

## 🛠️ Tech Stack

- **Spring Boot**: Modern Java framework for backend development
- **PostgreSQL with PostGIS**: Spatial database for location-based queries
- **Redis**: For high-performance caching
- **Hibernate Spatial**: For geospatial data handling
- **JDK 21**: Latest Java features
- **Docker**: For containerization
- **Fly.io**: For cloud deployment
- **GitHub Actions**: For CI/CD automation

## 🚀 Getting Started

### Prerequisites

- JDK 21
- Maven 3.9+
- PostgreSQL with PostGIS extension
- Redis (optional for caching)

### Environment Setup

Clone the repository and create a `.env` file in the project root based on the provided `.env.example`:

```bash
# Clone the repository
git clone https://github.com/boyrazcan33/OnnOto-backend.git
cd OnnOto-backend

# Create and edit .env file
cp onnoto-backend/.env.example onnoto-backend/.env
```

Edit the `.env` file with your database and Redis credentials. Here's an example of what your `.env` file should look like:

```bash
# Database Configuration
ONNOTO_DB_URL=jdbc:postgresql://localhost:5434/onnoto
ONNOTO_DB_USERNAME=your_username
ONNOTO_DB_PASSWORD=your_password

# Redis Configuration
ONNOTO_REDIS_HOST=your-redis-host.redns.redis-cloud.com
ONNOTO_REDIS_PORT=16388
ONNOTO_REDIS_PASSWORD=your-redis-password
ONNOTO_REDIS_APIKEY=your-redis-api-key
ONNOTO_REDIS_USERNAME=default

# API Keys for Data Providers
OPENCHARGE_API_KEY=your-opencharge-api-key
GOOGLE_PLACES_API_KEY=your-google-places-api-key
```

**Important:** Never commit your actual `.env` file to version control. The `.env` file is included in `.gitignore` to prevent accidental exposure of credentials.

### Build & Run

```bash
# Navigate to backend directory
cd onnoto-backend

# Build the project
./mvnw clean package

# Run the application
./mvnw spring-boot:run
```

### Docker Deployment

```bash
# Build Docker image
docker build -t onnoto-backend .

# Run Docker container
docker run -p 8087:8087 --env-file onnoto-backend/.env onnoto-backend
```

### Fly.io Deployment

The project includes configuration for Fly.io deployment:

```bash
# Deploy to Fly.io
flyctl deploy
```

## 📊 API Overview

### Station Data

- `GET /api/stations` - List all charging stations
- `GET /api/stations/{id}` - Get detailed station information
- `GET /api/stations/city/{city}` - Filter stations by city
- `GET /api/stations/nearby` - Find stations near coordinates

### Reliability & Analytics

- `GET /api/reliability/station/{stationId}` - Get station reliability metrics
- `GET /api/reliability/most-reliable` - Get top reliable stations
- `GET /api/anomalies` - View detected anomalies
- `GET /api/visualizations/reliability/distribution` - Get reliability distribution data

### Connector Status

- `GET /api/connectors/station/{stationId}` - Get connectors for a station
- `GET /api/connectors/type/{connectorType}` - Get connectors by type

## 🏗️ Project Structure

```
onnoto-backend/
├── src/main/java/com/onnoto/onnoto_backend/
│   ├── analytics/        # Reliability calculation & anomaly detection
│   ├── config/           # Application configuration
│   ├── controller/       # REST API endpoints
│   ├── db/               # Database migrations
│   ├── dto/              # Data transfer objects
│   ├── exception/        # Exception handling
│   ├── ingestion/        # Data ingestion services
│   ├── model/            # Data models/entities
│   ├── repository/       # Data access layer
│   └── service/          # Business logic
└── src/test/            # Test suite
```

## 🧪 Testing

```bash
# Run all tests
./mvnw test

# Run specific test
./mvnw test -Dtest=StationServiceTest
```

## 🔍 Monitoring

The application includes Spring Actuator endpoints for monitoring:

```
/actuator/health - Health check
/actuator/info - Application info
```

## 📄 License

Copyright (c) 2025 OnnOto

---

Made with ❤️ for Estonian EV drivers
