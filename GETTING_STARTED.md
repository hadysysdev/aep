# Getting Started Guide

This guide will help you set up your development environment and get the Agri Enhancement Platform (AEP) running locally.

## Prerequisites

Before you begin, ensure you have the following installed:

- **Java 21** - OpenJDK or Oracle JDK
- **Maven 3.9+** - For building the project
- **Docker & Docker Compose** - For running PostgreSQL and Keycloak in containers
- **Git** - For version control
- **IDE** - IntelliJ IDEA or Eclipse with Java and Maven support

## Project Structure

The AEP is a multi-module Maven project with the following structure:

```
agri-enhancement-platform/
├── pom.xml                          # Parent POM
├── README.md                        # Basic build commands
├── Docs/                            # Documentation
│   ├── # Project Overview.md
│   ├── # Technical Architecture.md
│   ├── # Functional Specification.md
│   └── ...
├── farm-plot-service/               # Farm & Plot Management Microservice
│   ├── pom.xml
│   └── src/
└── identity-access-service/         # Identity & Access Management Microservice
    ├── pom.xml
    └── src/
```

## Quick Start

### 1. Clone the Repository

```bash
git clone <repository-url>
cd agri-enhancement-platform
```

### 2. Start Required Services

Start PostgreSQL and Keycloak using Docker Compose:

```bash
# Create docker-compose.yml for development
cat > docker-compose.dev.yml << 'EOF'
version: '3.8'
services:
  postgres:
    image: postgis/postgis:15-3.3
    environment:
      POSTGRES_DB: aep_dev
      POSTGRES_USER: aep_user
      POSTGRES_PASSWORD: aep_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  keycloak:
    image: quay.io/keycloak/keycloak:25.0.2
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
      KC_HOSTNAME: localhost
      KC_HOSTNAME_PORT: 8080
    ports:
      - "8080:8080"
    command: start-dev

volumes:
  postgres_data:
EOF

# Start the services
docker-compose -f docker-compose.dev.yml up -d
```

### 3. Build the Project

```bash
# Build all modules
mvn clean install -DskipTests

# Or build with tests
mvn clean verify
```

### 4. Run the Services

#### Farm Plot Service

```bash
cd farm-plot-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The service will be available at: http://localhost:8081

#### Identity Access Service

```bash
cd identity-access-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The service will be available at: http://localhost:8082

## Development Workflow

### 1. Making Changes

1. Create a new feature branch from `main`
2. Make your changes to the relevant service
3. Run tests to ensure everything works
4. Commit your changes with descriptive messages

### 2. Running Tests

```bash
# Run all tests
mvn test

# Run tests for specific module
mvn test -pl farm-plot-service

# Run integration tests
mvn verify -P integration-tests
```

### 3. Database Migrations

The project uses Flyway for database migrations:

```bash
# For farm-plot-service
cd farm-plot-service
mvn flyway:migrate

# For identity-access-service  
cd identity-access-service
mvn flyway:migrate
```

## Development Profiles

The application supports these Spring profiles:

- `dev` - Local development with local PostgreSQL
- `test` - For running tests with Testcontainers
- `prod` - Production configuration

Configure your application properties in:
- `farm-plot-service/src/main/resources/application-dev.yml`
- `identity-access-service/src/main/resources/application-dev.yml`

## Useful Commands

```bash
# Build without running tests
mvn clean install -DskipTests

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Check for dependency updates
mvn versions:display-dependency-updates

# Generate dependency tree
mvn dependency:tree

# Clean build artifacts
mvn clean
```

## IDE Setup

### IntelliJ IDEA

1. Open the project as a Maven project
2. Ensure JDK 21 is set as the project SDK
3. Enable annotation processing for Lombok and MapStruct
4. Install the Lombok plugin

### Eclipse

1. Import as an existing Maven project
2. Configure the build path to use JDK 21
3. Install Lombok and configure for annotation processing

## Troubleshooting

### Port Conflicts

If you encounter port conflicts, you can change the default ports:

```bash
# For farm-plot-service
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081

# For identity-access-service
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8082
```

### Database Connection Issues

Ensure PostgreSQL is running and accessible:
- Check Docker containers: `docker ps`
- Verify database exists: Connect to PostgreSQL and list databases
- Check connection properties in application-dev.yml

### Build Failures

If Maven build fails:
1. Check your Java version: `java -version` (should be 21)
2. Clear Maven cache: `mvn dependency:purge-local-repository`
3. Rebuild from clean state: `mvn clean install`

## Next Steps

Once you have the environment set up:

1. Read the [Project Overview](Docs/#%20Project%20Overview.md) to understand the platform
2. Review the [Technical Architecture](Docs/#%20Technical%20Architecture.md) for system design
3. Check the [Task Decomposition](Docs/#%20Task%20Decomposition.md) for current development tasks
4. Explore the service-specific documentation in the `Docs/` folder

## Getting Help

If you encounter issues:

1. Check the troubleshooting section above
2. Review the existing documentation in the `Docs/` folder
3. Look at test cases for usage examples
4. Check the console logs for detailed error messages
