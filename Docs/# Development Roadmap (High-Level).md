# Development Roadmap (High-Level)

## Milestone 1: Foundational Farm & Plot Management (Complete)
*   **Status:** Done.
*   **Description:** The core `farm-plot-service` has been designed and skeleton-coded. This includes a multi-module Maven setup, entities, repositories, services, DTOs, mappers, and a comprehensive suite of unit and integration tests using Testcontainers.

## Milestone 2: Identity & Access Management (IAM)
*   **Priority:** High.
*   **Description:** Design and build the `identity-access-service`. This service is a prerequisite for securing other services and managing multi-tenancy properly. It will handle user registration, login (issuing JWTs), and role/permission management.

## Milestone 3: API Gateway & Security Integration
*   **Priority:** High.
*   **Description:** Set up the API Gateway (e.g., Spring Cloud Gateway). Integrate it with the IAM service to secure all endpoints. The `farm-plot-service` will be updated to consume tenant and user information from the security context (JWT).

## Milestone 4: Agricultural Practices & Core Business Logic
*   **Priority:** Medium.
*   **Description:** Design and build the `agricultural-practices-service` to manage crop cycles, activities, and observations. This will be the first service to heavily interact with the `farm-plot-service`.

## Milestone 5: Client Applications (Initial Versions)
*   **Priority:** Medium.
*   **Description:** Develop initial versions of the Web Portal and Mobile App to interact with the existing backend services.