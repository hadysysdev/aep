# Agri Enhancement Platform (AEP)

A comprehensive, cloud-native software platform that empowers African agriculture at all levels—from smallholder farmers to large cooperatives.

## 🚀 Quick Start

### Prerequisites

- Java 21
- Maven 3.9+
- Docker & Docker Compose
- PostgreSQL with PostGIS
- Keycloak

### Development Setup

1. **Clone and Build**
   ```bash
   git clone <repository-url>
   cd agri-enhancement-platform
   mvn clean install -DskipTests
   ```

2. **Start Infrastructure**
   ```bash
   docker-compose -f docker-compose.dev.yml up -d
   ```

3. **Run Services**
   ```bash
   # Farm Plot Service
   cd farm-plot-service
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   
   # Identity Access Service
   cd ../identity-access-service
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

For detailed setup instructions, see the [Getting Started Guide](GETTING_STARTED.md).

## 📚 Documentation

### Essential Reading
- [Getting Started Guide](GETTING_STARTED.md) - Development environment setup
- [Deployment Guide](DEPLOYMENT.md) - Production deployment instructions
- [Contributing Guide](CONTRIBUTING.md) - How to contribute to the project
- [API Documentation](Docs/) - Complete API reference

### Project Documentation
- [Project Overview](Docs/#%20Project%20Overview.md) - Vision and principles
- [Technical Architecture](Docs/#%20Technical%20Architecture.md) - System design
- [Functional Specification](Docs/#%20Functional%20Specification%20(High-Level).md) - Feature specifications
- [Development Roadmap](Docs/#%20Development%20Roadmap%20(High-Level).md) - Project milestones

### Service Documentation
- [Farm Plot Service API](Docs/FarmPlot_Doc/#%20API%20Contract%20(%60farm-plot-service%60).md)
- [Farm Plot Data Model](Docs/FarmPlot_Doc/#%20Data%20Model%20&%20Schema%20(%60farm-plot-servic.md)
- [IAM Service API](Docs/IAM_Doc/#%20API%20Contract%20(identity-access-service).md)
- [IAM Data Model](Docs/IAM_Doc/#%20Data%20Model%20&%20Schema%20(identity-access-service).md)

## 🏗️ Architecture

AEP follows a microservices architecture with:

- **farm-plot-service** - Farm and plot management with geospatial support
- **identity-access-service** - User authentication and multi-tenant management
- **API Gateway** - Centralized routing and security (planned)
- **PostgreSQL + PostGIS** - Spatial database for geographic data
- **Keycloak** - Identity and access management

## 🛠️ Development

### Build Commands

```bash
# Build all modules
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Run tests
mvn verify

# Run specific module tests
mvn test -pl farm-plot-service
```

### Code Quality

The project uses:
- **Lombok** - Reduce boilerplate code
- **MapStruct** - Type-safe bean mapping
- **Testcontainers** - Integration testing with real databases
- **Spring Boot Actuator** - Health checks and monitoring

## 📋 Current Status

### ✅ Completed
- Project structure and documentation
- Farm Plot Service (skeleton with entities, repositories, APIs)
- Identity Access Service (basic structure)
- Comprehensive test suite
- Docker development environment

### 🚧 In Progress
- Complete IAM service implementation
- API Gateway integration
- Enhanced security features

### 📅 Planned
- Agricultural Practices service
- IoT data ingestion
- Mobile and web applications

## 🤝 Contributing

We welcome contributions! Please read our [Contributing Guide](CONTRIBUTING.md) to get started.

### Quick Contribution Steps

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests and documentation
5. Submit a pull request

## 📄 License

This project is licensed under the terms specified in the [LICENSE](LICENSE) file.

## 🆘 Support

- Check the [documentation](Docs/) first
- Search [existing issues](../../issues)
- Contact the maintainers for questions

---

Built with ❤️ for African agriculture