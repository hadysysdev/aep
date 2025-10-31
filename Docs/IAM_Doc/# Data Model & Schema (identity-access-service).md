# Data Model & Schema (identity-access-service)

This document describes the database schema and data model for the Identity & Access Management service.

## Overview

The IAM service uses PostgreSQL as its primary database for storing tenant metadata, user information, and role assignments. The actual user credentials and authentication data are stored in Keycloak's internal database.

## Database Schema

### Primary Tables

#### tenants
Stores metadata about AEP tenants, which correspond to Keycloak realms.

```sql
CREATE TABLE tenants (
    tenant_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    domain VARCHAR(100),
    keycloak_realm_id VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('ACTIVE', 'SUSPENDED', 'PENDING')),
    settings JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

-- Indexes
CREATE INDEX idx_tenants_status ON tenants(status);
CREATE INDEX idx_tenants_domain ON tenants(domain) WHERE domain IS NOT NULL;
CREATE INDEX idx_tenants_keycloak_realm ON tenants(keycloak_realm_id);
```

#### users
Stores user metadata synchronized from Keycloak.

```sql
CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(tenant_id) ON DELETE CASCADE,
    keycloak_user_id VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(255),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone_number VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING', 'SUSPENDED')),
    email_verified BOOLEAN DEFAULT FALSE,
    phone_number_verified BOOLEAN DEFAULT FALSE,
    attributes JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_login_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    
    UNIQUE(tenant_id, username)
);

-- Indexes
CREATE INDEX idx_users_tenant_id ON users(tenant_id);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email) WHERE email IS NOT NULL;
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_keycloak_user ON users(keycloak_user_id);
```

#### roles
Defines roles available within a tenant.

```sql
CREATE TABLE roles (
    role_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(tenant_id) ON DELETE CASCADE,
    keycloak_role_id VARCHAR(255) NOT NULL,
    name VARCHAR(50) NOT NULL,
    description TEXT,
    is_system_role BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    
    UNIQUE(tenant_id, name),
    UNIQUE(tenant_id, keycloak_role_id)
);

-- Indexes
CREATE INDEX idx_roles_tenant_id ON roles(tenant_id);
CREATE INDEX idx_roles_name ON roles(name);
CREATE INDEX idx_roles_system_role ON roles(is_system_role);
```

#### permissions
Defines system-wide permissions.

```sql
CREATE TABLE permissions (
    permission_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    category VARCHAR(50),
    is_system_permission BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_permissions_category ON permissions(category);
CREATE INDEX idx_permissions_system ON permissions(is_system_permission);
```

#### role_permissions
Maps permissions to roles (many-to-many relationship).

```sql
CREATE TABLE role_permissions (
    role_id UUID NOT NULL REFERENCES roles(role_id) ON DELETE CASCADE,
    permission_id VARCHAR(50) NOT NULL REFERENCES permissions(permission_id) ON DELETE CASCADE,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    granted_by UUID REFERENCES users(user_id),
    PRIMARY KEY (role_id, permission_id)
);

-- Indexes
CREATE INDEX idx_role_permissions_role ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission ON role_permissions(permission_id);
```

#### user_roles
Maps users to roles (many-to-many relationship).

```sql
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(role_id) ON DELETE CASCADE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    assigned_by UUID REFERENCES users(user_id),
    expires_at TIMESTAMPTZ,
    PRIMARY KEY (user_id, role_id)
);

-- Indexes
CREATE INDEX idx_user_roles_user ON user_roles(user_id);
CREATE INDEX idx_user_roles_role ON user_roles(role_id);
CREATE INDEX idx_user_roles_expires ON user_roles(expires_at) WHERE expires_at IS NOT NULL;
```

### Audit and History Tables

#### user_sessions
Tracks user login sessions for audit purposes.

```sql
CREATE TABLE user_sessions (
    session_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    keycloak_session_id VARCHAR(255) NOT NULL,
    client_id VARCHAR(255),
    ip_address INET,
    user_agent TEXT,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_accessed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    is_active BOOLEAN DEFAULT TRUE
);

-- Indexes
CREATE INDEX idx_user_sessions_user ON user_sessions(user_id);
CREATE INDEX idx_user_sessions_active ON user_sessions(is_active) WHERE is_active = TRUE;
CREATE INDEX idx_user_sessions_expires ON user_sessions(expires_at);
```

#### audit_logs
Stores audit trail for all tenant and user management operations.

```sql
CREATE TABLE audit_logs (
    log_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES tenants(tenant_id),
    user_id UUID REFERENCES users(user_id),
    action VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id VARCHAR(255),
    old_values JSONB,
    new_values JSONB,
    ip_address INET,
    user_agent TEXT,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_audit_logs_tenant ON audit_logs(tenant_id);
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_resource ON audit_logs(resource_type, resource_id);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp);
```

## Data Relationships

### Entity Relationship Diagram

```
tenants (1) -----> (N) users
tenants (1) -----> (N) roles
users (N) <-----> (N) roles (through user_roles)
roles (N) <-----> (N) permissions (through role_permissions)
tenants (1) -----> (N) audit_logs
users (1) -----> (N) audit_logs
users (1) -----> (N) user_sessions
```

### Key Constraints

1. **Tenant Isolation**: All user and role data is scoped to a specific tenant
2. **Unique Constraints**: Usernames are unique within a tenant
3. **Referential Integrity**: Foreign keys ensure data consistency
4. **Audit Trail**: All modifications are tracked in audit_logs

## Initial Data

### Default Permissions

```sql
INSERT INTO permissions (permission_id, name, description, category) VALUES
-- Farm Management
('FARM_CREATE', 'Create Farm', 'Create new farms', 'FARM'),
('FARM_READ', 'Read Farm', 'View farm details', 'FARM'),
('FARM_UPDATE', 'Update Farm', 'Update farm information', 'FARM'),
('FARM_DELETE', 'Delete Farm', 'Delete farms', 'FARM'),

-- Plot Management
('PLOT_CREATE', 'Create Plot', 'Create new plots', 'PLOT'),
('PLOT_READ', 'Read Plot', 'View plot details', 'PLOT'),
('PLOT_UPDATE', 'Update Plot', 'Update plot information', 'PLOT'),
('PLOT_DELETE', 'Delete Plot', 'Delete plots', 'PLOT'),

-- User Management
('USER_CREATE', 'Create User', 'Create new users', 'USER'),
('USER_READ', 'Read User', 'View user information', 'USER'),
('USER_UPDATE', 'Update User', 'Update user information', 'USER'),
('USER_DELETE', 'Delete User', 'Delete users', 'USER'),
('USER_INVITE', 'Invite User', 'Invite users to tenant', 'USER'),

-- Role Management
('ROLE_CREATE', 'Create Role', 'Create new roles', 'ROLE'),
('ROLE_READ', 'Read Role', 'View role information', 'ROLE'),
('ROLE_UPDATE', 'Update Role', 'Update role information', 'ROLE'),
('ROLE_DELETE', 'Delete Role', 'Delete roles', 'ROLE'),
('ROLE_ASSIGN', 'Assign Role', 'Assign roles to users', 'ROLE'),

-- Tenant Management
('TENANT_CREATE', 'Create Tenant', 'Create new tenants', 'TENANT'),
('TENANT_READ', 'Read Tenant', 'View tenant information', 'TENANT'),
('TENANT_UPDATE', 'Update Tenant', 'Update tenant information', 'TENANT'),
('TENANT_DELETE', 'Delete Tenant', 'Delete tenants', 'TENANT'),

-- Platform Administration
('PLATFORM_ADMIN', 'Platform Admin', 'Full platform access', 'PLATFORM'),
('SYSTEM_MONITOR', 'System Monitor', 'Access system monitoring', 'PLATFORM'),
('SYSTEM_CONFIG', 'System Config', 'Modify system configuration', 'PLATFORM');
```

### Default System Roles

When a new tenant is created, these default roles are automatically created:

```sql
-- These would be created programmatically when a tenant is onboarded

-- Tenant Administrator Role
-- Permissions: All tenant-level permissions except platform admin

-- Field Agent Role  
-- Permissions: FARM_READ, FARM_CREATE, PLOT_READ, PLOT_CREATE, USER_READ

-- Farmer Role
-- Permissions: FARM_READ (own farms), PLOT_READ (own plots)

-- Cooperative Manager Role
-- Permissions: All farm/plot permissions, USER_READ, USER_INVITE
```

## JSONB Schema Examples

### Tenant Settings

```json
{
  "maxUsers": 1000,
  "defaultRoles": ["FARMER"],
  "passwordPolicy": {
    "minLength": 8,
    "requireUppercase": true,
    "requireLowercase": true,
    "requireNumbers": true,
    "requireSpecialChars": true
  },
  "sessionSettings": {
    "maxSessions": 3,
    "sessionTimeout": 480,
    "idleTimeout": 60
  },
  "integrationSettings": {
    "smsEnabled": true,
    "emailEnabled": true,
    "apiRateLimit": 100
  }
}
```

### User Attributes

```json
{
  "profile": {
    "avatarUrl": "https://example.com/avatars/user123.jpg",
    "bio": "Small-scale farmer specializing in maize cultivation",
    "location": {
      "country": "Kenya",
      "region": "Rift Valley",
      "coordinates": [1.2921, 36.8219]
    }
  },
  "preferences": {
    "language": "en",
    "timezone": "Africa/Nairobi",
    "dateFormat": "DD/MM/YYYY",
    "notifications": {
      "email": true,
      "sms": false,
      "push": true
    }
  },
  "verification": {
    "identityVerified": true,
    "identityDocument": "PASSPORT",
    "verificationDate": "2024-01-15T10:30:00Z"
  }
}
```

## Database Views

### User Details View

```sql
CREATE VIEW user_details AS
SELECT 
    u.user_id,
    u.tenant_id,
    u.username,
    u.email,
    u.first_name,
    u.last_name,
    u.status,
    u.created_at,
    u.last_login_at,
    t.name as tenant_name,
    ARRAY_AGG(r.name) as roles,
    COUNT(ur.role_id) as role_count
FROM users u
JOIN tenants t ON u.tenant_id = t.tenant_id
LEFT JOIN user_roles ur ON u.user_id = ur.user_id
LEFT JOIN roles r ON ur.role_id = r.role_id
GROUP BY u.user_id, u.tenant_id, u.username, u.email, u.first_name, u.last_name, u.status, u.created_at, u.last_login_at, t.name;
```

### Role Permissions View

```sql
CREATE VIEW role_permissions_details AS
SELECT 
    r.role_id,
    r.tenant_id,
    r.name as role_name,
    r.description as role_description,
    ARRAY_AGG(p.permission_id) as permissions,
    COUNT(p.permission_id) as permission_count
FROM roles r
LEFT JOIN role_permissions rp ON r.role_id = rp.role_id
LEFT JOIN permissions p ON rp.permission_id = p.permission_id
GROUP BY r.role_id, r.tenant_id, r.name, r.description;
```

## Triggers and Functions

### Update Timestamp Trigger

```sql
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply to tables that need updated_at triggers
CREATE TRIGGER update_tenants_updated_at BEFORE UPDATE ON tenants
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_roles_updated_at BEFORE UPDATE ON roles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

### Audit Log Trigger

```sql
CREATE OR REPLACE FUNCTION audit_trigger_function()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO audit_logs (action, resource_type, resource_id, new_values)
        VALUES ('CREATE', TG_TABLE_NAME, NEW.id::text, row_to_json(NEW));
        RETURN NEW;
    ELSIF TG_OP = 'UPDATE' THEN
        INSERT INTO audit_logs (action, resource_type, resource_id, old_values, new_values)
        VALUES ('UPDATE', TG_TABLE_NAME, NEW.id::text, row_to_json(OLD), row_to_json(NEW));
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        INSERT INTO audit_logs (action, resource_type, resource_id, old_values)
        VALUES ('DELETE', TG_TABLE_NAME, OLD.id::text, row_to_json(OLD));
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Apply audit triggers
CREATE TRIGGER audit_tenants_trigger
    AFTER INSERT OR UPDATE OR DELETE ON tenants
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

CREATE TRIGGER audit_users_trigger
    AFTER INSERT OR UPDATE OR DELETE ON users
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();
```

## Migration Strategy

### Version 1.0.0

This schema represents the initial version for the IAM service. Future migrations will:

1. **Add new permissions** as new features are implemented
2. **Extend user attributes** schema as new user properties are needed
3. **Add audit enhancements** for compliance requirements
4. **Optimize indexes** based on query patterns
5. **Add partitioning** for large tables (audit_logs, user_sessions)

### Backward Compatibility

All schema changes will maintain backward compatibility through:

1. **Additive changes** only (new columns, new tables)
2. **Default values** for new required columns
3. **Deprecation warnings** before removing features
4. **Migration scripts** with rollback capabilities

## Performance Considerations

### Indexing Strategy

1. **Primary indexes** on all foreign keys
2. **Composite indexes** on frequently queried column combinations
3. **Partial indexes** for filtered queries
4. **JSONB indexes** for attribute searches

### Query Optimization

1. **Use views** for complex queries
2. **Implement caching** for frequently accessed data
3. **Optimize joins** with proper indexing
4. **Monitor slow queries** and optimize accordingly

This data model provides a comprehensive foundation for the IAM service while maintaining flexibility for future enhancements.