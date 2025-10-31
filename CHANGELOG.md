# Changelog

All notable changes to the Agri Enhancement Platform (AEP) will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Initial project structure and documentation
- Farm & Plot Management microservice skeleton
- Identity & Access Management microservice skeleton
- Comprehensive technical architecture documentation
- Development roadmap and task decomposition

### Changed
- N/A

### Deprecated
- N/A

### Removed
- N/A

### Fixed
- N/A

### Security
- N/A

## [0.0.1-SNAPSHOT] - 2024-10-31

### Added
- **Project Foundation**
  - Multi-module Maven setup for microservices architecture
  - Parent POM with dependency management for Spring Boot 3.5, Java 21
  - Support for PostgreSQL with PostGIS extension
  - Integration with Keycloak for identity management
  - Testcontainers configuration for integration testing

- **Farm Plot Service**
  - Core domain entities: Farm, Plot, LandTenure, PointOfInterest
  - Spring Data JPA repositories with spatial queries
  - REST API controllers for CRUD operations
  - MapStruct DTOs and mappers
  - Flyway database migrations
  - Comprehensive unit and integration test suite

- **Identity Access Service**
  - Basic service structure for Keycloak integration
  - Tenant management capabilities
  - User and role management foundations
  - Multi-tenancy support design

- **Documentation**
  - Project overview and vision
  - Technical architecture with C4 models
  - Functional specifications
  - API contracts and data models
  - Development roadmap
  - Task decomposition
  - Glossary of terms

- **Development Infrastructure**
  - Docker Compose configuration for local development
  - Testcontainers setup for PostgreSQL and Keycloak
  - Lombok and MapStruct annotation processing
  - OpenAPI documentation support
  - Actuator endpoints for health monitoring

### Changed
- N/A

### Deprecated
- N/A

### Removed
- N/A

### Fixed
- N/A

### Security
- N/A

---

## Version History

### Version 0.0.1-SNAPSHOT (Current)
- **Status**: Development/Alpha
- **Features**: Core farm plot management, basic IAM structure
- **Stability**: Not production ready
- **Next Milestone**: Complete IAM service implementation

### Planned Future Versions

#### Version 0.1.0 (Alpha)
- Complete IAM service implementation
- API Gateway integration
- Basic security between services
- Enhanced testing coverage

#### Version 0.2.0 (Beta)
- Agricultural practices service
- IoT data ingestion foundation
- Improved documentation
- Performance optimizations

#### Version 1.0.0 (Production)
- Full feature set as specified in roadmap
- Production-ready deployment configurations
- Comprehensive monitoring and observability
- Security audit completed
- Performance benchmarks established

---

## Breaking Changes

This section will document any breaking changes in future releases.

### Future Breaking Changes (Planned)

- **IAM Service v0.2.0**: Changes to authentication flow when API Gateway is introduced
- **Database v1.0.0**: Final database schema with migration path from development schema
- **API v1.0.0**: Stabilized API contracts with versioning strategy

---

## Migration Guide

This section will contain migration guides for major version updates.

### From 0.0.x to 0.1.0 (Planned)
- Update Keycloak configuration for realm-per-tenant model
- Migrate authentication endpoints to API Gateway
- Update service-to-service communication patterns

---

## Security Updates

This section will track security-related updates and patches.

### Security Advisories

No security advisories have been issued yet.

Future security updates will be documented here with:
- CVE numbers (if applicable)
- Description of vulnerability
- Affected versions
- Recommended actions

---

## Dependencies

### Key Dependencies and Their Versions

| Dependency | Version | Status |
|------------|---------|--------|
| Spring Boot | 3.5.0 | Current |
| Java | 21 | Current |
| PostgreSQL | 15+ | Current |
| PostGIS | 3.3+ | Current |
| Keycloak | 25.0.2 | Current |
| Testcontainers | 1.21.1 | Current |
| MapStruct | 1.6.3 | Current |
| Lombok | 1.18.38 | Current |

### Dependency Updates

Recent dependency updates will be tracked here in future releases.

---

## Performance Metrics

This section will track performance improvements and benchmarks in future releases.

### Baseline Metrics (Planned for v0.1.0)
- API response times
- Database query performance
- Memory usage patterns
- Startup times

---

## Contributors

This section will acknowledge contributors in future releases.

### Current Contributors
- Project maintainers and initial development team

---

## How to Update This Changelog

When making changes to the project:

1. Add entries to the "Unreleased" section
2. Categorize changes (Added, Changed, Deprecated, Removed, Fixed, Security)
3. Use clear, concise descriptions
4. Reference relevant issue numbers when applicable
5. Create a new release section when publishing

### Example Entry

```markdown
### Added
- New plot geometry validation endpoint (#123)
- Support for bulk farm creation (#124)

### Fixed
- Corrected tenant isolation bug in plot queries (#125)
- Fixed memory leak in large dataset processing (#126)
```

---

## Support and Feedback

For questions about changes or to provide feedback:

- Check the [documentation](Docs/)
- Review [GitHub issues](../../issues)
- Contact the maintainers

---

*This changelog follows the principles of [Keep a Changelog](https://keepachangelog.com/)*