# Deployment Guide

This guide covers deploying the Agri Enhancement Platform (AEP) to various environments, from local development to production.

## Architecture Overview

AEP consists of multiple microservices that can be deployed independently or together:

- **farm-plot-service** - Farm and plot management (Port 8081)
- **identity-access-service** - User authentication and authorization (Port 8082)
- **PostgreSQL with PostGIS** - Primary database (Port 5432)
- **Keycloak** - Identity provider (Port 8080)

## Deployment Options

### 1. Docker Compose (Recommended for Development/Staging)

#### Basic Deployment

Create `docker-compose.yml`:

```yaml
version: '3.8'

services:
  postgres:
    image: postgis/postgis:15-3.3
    environment:
      POSTGRES_DB: ${POSTGRES_DB:-aep}
      POSTGRES_USER: ${POSTGRES_USER:-aep_user}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-secure_password}
    ports:
      - "${POSTGRES_PORT:-5432}:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init-scripts:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-aep_user} -d ${POSTGRES_DB:-aep}"]
      interval: 30s
      timeout: 10s
      retries: 3

  keycloak:
    image: quay.io/keycloak/keycloak:25.0.2
    environment:
      KEYCLOAK_ADMIN: ${KEYCLOAK_ADMIN:-admin}
      KEYCLOAK_ADMIN_PASSWORD: ${KEYCLOAK_ADMIN_PASSWORD:-admin}
      KC_HOSTNAME: ${KC_HOSTNAME:-localhost}
      KC_HOSTNAME_PORT: ${KC_HOSTNAME_PORT:-8080}
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB:-aep}
      KC_DB_USERNAME: ${POSTGRES_USER:-aep_user}
      KC_DB_PASSWORD: ${POSTGRES_PASSWORD:-secure_password}
    ports:
      - "${KEYCLOAK_PORT:-8080}:8080"
    depends_on:
      postgres:
        condition: service_healthy
    command: start-dev
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/health/ready"]
      interval: 30s
      timeout: 10s
      retries: 3

  farm-plot-service:
    build: ./farm-plot-service
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES:-docker}
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB:-aep}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER:-aep_user}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD:-secure_password}
      KEYCLOAK_SERVER_URL: http://keycloak:8080
    ports:
      - "${FARM_PLOT_SERVICE_PORT:-8081}:8081"
    depends_on:
      postgres:
        condition: service_healthy
      keycloak:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  identity-access-service:
    build: ./identity-access-service
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES:-docker}
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB:-aep}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER:-aep_user}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD:-secure_password}
      KEYCLOAK_SERVER_URL: http://keycloak:8080
    ports:
      - "${IDENTITY_SERVICE_PORT:-8082}:8082"
    depends_on:
      postgres:
        condition: service_healthy
      keycloak:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8082/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

volumes:
  postgres_data:
```

Create `.env` file for environment-specific values:

```bash
# Database Configuration
POSTGRES_DB=aep_prod
POSTGRES_USER=aep_user
POSTGRES_PASSWORD=your_secure_password_here
POSTGRES_PORT=5432

# Keycloak Configuration
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=your_admin_password_here
KC_HOSTNAME=your-domain.com
KC_HOSTNAME_PORT=8080
KEYCLOAK_PORT=8080

# Service Ports
FARM_PLOT_SERVICE_PORT=8081
IDENTITY_SERVICE_PORT=8082

# Spring Profiles
SPRING_PROFILES=docker
```

#### Deploy with Docker Compose

```bash
# Build and start all services
docker-compose up -d --build

# View logs
docker-compose logs -f

# Stop services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

### 2. Kubernetes Deployment

#### Namespace and ConfigMaps

```yaml
# namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: aep

---
# configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: aep-config
  namespace: aep
data:
  POSTGRES_DB: "aep"
  POSTGRES_USER: "aep_user"
  KEYCLOAK_ADMIN: "admin"
  KC_HOSTNAME: "keycloak.aep.svc.cluster.local"
```

#### Secrets

```yaml
# secrets.yaml
apiVersion: v1
kind: Secret
metadata:
  name: aep-secrets
  namespace: aep
type: Opaque
data:
  POSTGRES_PASSWORD: <base64-encoded-password>
  KEYCLOAK_ADMIN_PASSWORD: <base64-encoded-password>
```

#### PostgreSQL Deployment

```yaml
# postgres.yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres
  namespace: aep
spec:
  serviceName: postgres
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
      - name: postgres
        image: postgis/postgis:15-3.3
        env:
        - name: POSTGRES_DB
          valueFrom:
            configMapKeyRef:
              name: aep-config
              key: POSTGRES_DB
        - name: POSTGRES_USER
          valueFrom:
            configMapKeyRef:
              name: aep-config
              key: POSTGRES_USER
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: aep-secrets
              key: POSTGRES_PASSWORD
        ports:
        - containerPort: 5432
        volumeMounts:
        - name: postgres-storage
          mountPath: /var/lib/postgresql/data
  volumeClaimTemplates:
  - metadata:
      name: postgres-storage
    spec:
      accessModes: ["ReadWriteOnce"]
      resources:
        requests:
          storage: 20Gi

---
apiVersion: v1
kind: Service
metadata:
  name: postgres
  namespace: aep
spec:
  selector:
    app: postgres
  ports:
  - port: 5432
    targetPort: 5432
```

#### Application Services

```yaml
# farm-plot-service.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: farm-plot-service
  namespace: aep
spec:
  replicas: 2
  selector:
    matchLabels:
      app: farm-plot-service
  template:
    metadata:
      labels:
        app: farm-plot-service
    spec:
      containers:
      - name: farm-plot-service
        image: aep/farm-plot-service:latest
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "k8s"
        - name: SPRING_DATASOURCE_URL
          value: "jdbc:postgresql://postgres:5432/aep"
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            configMapKeyRef:
              name: aep-config
              key: POSTGRES_USER
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: aep-secrets
              key: POSTGRES_PASSWORD
        ports:
        - containerPort: 8081
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 10

---
apiVersion: v1
kind: Service
metadata:
  name: farm-plot-service
  namespace: aep
spec:
  selector:
    app: farm-plot-service
  ports:
  - port: 8081
    targetPort: 8081
  type: ClusterIP
```

#### Ingress Configuration

```yaml
# ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: aep-ingress
  namespace: aep
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
spec:
  tls:
  - hosts:
    - api.your-domain.com
    secretName: aep-tls
  rules:
  - host: api.your-domain.com
    http:
      paths:
      - path: /farm-plot
        pathType: Prefix
        backend:
          service:
            name: farm-plot-service
            port:
              number: 8081
      - path: /identity
        pathType: Prefix
        backend:
          service:
            name: identity-access-service
            port:
              number: 8082
```

## Environment Configuration

### Application Profiles

The services support different Spring profiles for various environments:

#### Development Profile (`application-dev.yml`)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/aep_dev
    username: aep_user
    password: dev_password
  
logging:
  level:
    com.agrienhance: DEBUG
    org.springframework.security: DEBUG
```

#### Docker Profile (`application-docker.yml`)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/aep
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASSWORD}
  
keycloak:
  server-url: http://keycloak:8080
```

#### Production Profile (`application-prod.yml`)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/aep
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
  
logging:
  level:
    com.agrienhance: INFO
  file:
    name: /app/logs/application.log
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `dev` |
| `POSTGRES_DB` | Database name | `aep` |
| `POSTGRES_USER` | Database username | `aep_user` |
| `POSTGRES_PASSWORD` | Database password | - |
| `KEYCLOAK_SERVER_URL` | Keycloak server URL | `http://localhost:8080` |
| `KEYCLOAK_ADMIN` | Keycloak admin username | `admin` |
| `KEYCLOAK_ADMIN_PASSWORD` | Keycloak admin password | - |

## Monitoring and Health Checks

### Actuator Endpoints

All services expose Spring Boot Actuator endpoints:

- `/actuator/health` - Health status
- `/actuator/metrics` - Application metrics
- `/actuator/info` - Application information
- `/actuator/env` - Environment properties

### Health Check Configuration

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized
```

## Database Management

### Migrations

Database migrations are handled by Flyway:

```bash
# Run migrations
mvn flyway:migrate

# Check migration status
mvn flyway:info

# Validate migrations
mvn flyway:validate
```

### Backups

#### PostgreSQL Backup

```bash
# Create backup
docker exec postgres pg_dump -U aep_user aep > backup.sql

# Restore backup
docker exec -i postgres psql -U aep_user aep < backup.sql
```

#### Automated Backup Script

```bash
#!/bin/bash
# backup.sh

BACKUP_DIR="/backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/aep_backup_$TIMESTAMP.sql"

mkdir -p $BACKUP_DIR

docker exec postgres pg_dump -U aep_user aep > $BACKUP_FILE

# Keep only last 7 days of backups
find $BACKUP_DIR -name "aep_backup_*.sql" -mtime +7 -delete

echo "Backup created: $BACKUP_FILE"
```

## Security Considerations

### SSL/TLS Configuration

For production deployments:

1. Use HTTPS for all external communications
2. Configure SSL certificates for Keycloak
3. Enable SSL for database connections
4. Use secure secrets management

### Network Security

- Use firewall rules to restrict database access
- Implement network policies in Kubernetes
- Use VPN for administrative access
- Regularly update dependencies and patch vulnerabilities

## Troubleshooting

### Common Issues

1. **Database Connection Failed**
   - Check if PostgreSQL is running
   - Verify connection strings and credentials
   - Ensure network connectivity

2. **Keycloak Connection Issues**
   - Verify Keycloak is accessible
   - Check realm configurations
   - Review client settings

3. **Service Startup Failures**
   - Check application logs
   - Verify environment variables
   - Ensure required services are healthy

### Log Locations

- Docker Compose: `docker-compose logs <service-name>`
- Kubernetes: `kubectl logs -f deployment/<service-name> -n aep`
- Application logs: Configured in application-{profile}.yml

## Performance Tuning

### Database Optimization

```sql
-- Add indexes for frequently queried columns
CREATE INDEX CONCURRENTLY idx_farms_tenant_id ON farms(tenant_id);
CREATE INDEX CONCURRENTLY idx_plots_farm_id ON plots(farm_identifier);

-- Analyze table statistics
ANALYZE farms;
ANALYZE plots;
```

### JVM Tuning

```bash
# Production JVM options
JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

This deployment guide provides the essential information for deploying AEP in various environments. Adjust configurations based on your specific requirements and infrastructure.
