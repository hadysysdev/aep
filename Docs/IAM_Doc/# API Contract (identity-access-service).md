# API Contract (identity-access-service)

**Base Path:** `/v1`

## Authentication

**Note:** Clients should use standard OIDC endpoints provided directly by Keycloak for authentication. This service provides tenant and user management APIs.

## Resource: `/tenants`

### `POST /tenants`
Onboard a new tenant (platform admin task).
- **Request Body:** `CreateTenantRequest`
- **Response:** `201 Created`, `TenantResponse`
- **Permissions:** `PLATFORM_ADMIN`

```json
{
  "name": "Cooperative ABC",
  "description": "Agricultural cooperative in Kenya",
  "domain": "abc.coop",
  "settings": {
    "maxUsers": 1000,
    "defaultRoles": ["FARMER", "FIELD_AGENT"]
  }
}
```

### `GET /tenants`
List all tenants (paginated).
- **Query Parameters:** `page`, `size`, `sort`
- **Response:** `200 OK`, `Page<TenantResponse>`
- **Permissions:** `PLATFORM_ADMIN`

### `GET /tenants/{tenantId}`
Get details for a specific tenant.
- **Response:** `200 OK`, `TenantResponse`
- **Permissions:** `PLATFORM_ADMIN` or tenant admin

### `PUT /tenants/{tenantId}`
Update tenant details.
- **Request Body:** `UpdateTenantRequest`
- **Response:** `200 OK`, `TenantResponse`
- **Permissions:** `PLATFORM_ADMIN` or tenant admin

### `DELETE /tenants/{tenantId}`
Deactivate a tenant.
- **Response:** `204 No Content`
- **Permissions:** `PLATFORM_ADMIN`

## Resource: `/tenants/{tenantId}/users`

### `POST /tenants/{tenantId}/users`
Create a new user within a tenant.
- **Request Body:** `CreateUserRequest`
- **Response:** `201 Created`, `UserResponse`
- **Permissions:** `TENANT_ADMIN`

```json
{
  "username": "john.doe",
  "email": "john.doe@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "temporaryPassword": "TempPass123!",
  "roles": ["FARMER"],
  "status": "ACTIVE"
}
```

### `GET /tenants/{tenantId}/users`
List all users for a tenant.
- **Query Parameters:** `page`, `size`, `sort`, `status`, `role`
- **Response:** `200 OK`, `Page<UserResponse>`
- **Permissions:** `TENANT_ADMIN` or `FIELD_AGENT`

### `GET /tenants/{tenantId}/users/{userId}`
Get details for a specific user.
- **Response:** `200 OK`, `UserResponse`
- **Permissions:** Own profile or `TENANT_ADMIN`

### `PUT /tenants/{tenantId}/users/{userId}`
Update a user's profile or status.
- **Request Body:** `UpdateUserRequest`
- **Response:** `200 OK`, `UserResponse`
- **Permissions:** Own profile (limited) or `TENANT_ADMIN`

### `DELETE /tenants/{tenantId}/users/{userId}`
Deactivate a user.
- **Response:** `204 No Content`
- **Permissions:** `TENANT_ADMIN`

### `POST /tenants/{tenantId}/users/{userId}/reset-password`
Reset user password.
- **Request Body:** `ResetPasswordRequest`
- **Response:** `200 OK`, `PasswordResetResponse`
- **Permissions:** `TENANT_ADMIN` or own profile

```json
{
  "newPassword": "NewSecurePass123!",
  "temporary": true
}
```

## Resource: `/tenants/{tenantId}/roles`

### `POST /tenants/{tenantId}/roles`
Create a new role for a tenant.
- **Request Body:** `CreateRoleRequest`
- **Response:** `201 Created`, `RoleResponse`
- **Permissions:** `TENANT_ADMIN`

```json
{
  "name": "COOPERATIVE_MANAGER",
  "description": "Can manage farms and users within the cooperative",
  "permissions": [
    "FARM_CREATE",
    "FARM_READ",
    "FARM_UPDATE",
    "USER_READ",
    "USER_INVITE"
  ]
}
```

### `GET /tenants/{tenantId}/roles`
List all roles for a tenant.
- **Response:** `200 OK`, `List<RoleResponse>`
- **Permissions:** `TENANT_ADMIN`

### `GET /tenants/{tenantId}/roles/{roleId}`
Get details for a specific role.
- **Response:** `200 OK`, `RoleResponse`
- **Permissions:** `TENANT_ADMIN`

### `PUT /tenants/{tenantId}/roles/{roleId}`
Update a role's name or description.
- **Request Body:** `UpdateRoleRequest`
- **Response:** `200 OK`, `RoleResponse`
- **Permissions:** `TENANT_ADMIN`

### `DELETE /tenants/{tenantId}/roles/{roleId}`
Delete a role.
- **Response:** `204 No Content`
- **Permissions:** `TENANT_ADMIN`

## Resource: `/tenants/{tenantId}/roles/{roleId}/permissions`

### `POST /tenants/{tenantId}/roles/{roleId}/permissions`
Grant a permission to a role.
- **Request Body:** `GrantPermissionRequest`
- **Response:** `200 OK`, `RoleResponse`
- **Permissions:** `TENANT_ADMIN`

```json
{
  "permissionId": "FARM_CREATE"
}
```

### `DELETE /tenants/{tenantId}/roles/{roleId}/permissions/{permissionId}`
Revoke a permission from a role.
- **Response:** `204 No Content`
- **Permissions:** `TENANT_ADMIN`

## Resource: `/tenants/{tenantId}/users/{userId}/roles`

### `POST /tenants/{tenantId}/users/{userId}/roles`
Assign a role to a user.
- **Request Body:** `AssignRoleRequest`
- **Response:** `200 OK`, `UserResponse`
- **Permissions:** `TENANT_ADMIN`

```json
{
  "roleId": "FIELD_AGENT"
}
```

### `DELETE /tenants/{tenantId}/users/{userId}/roles/{roleId}`
Revoke a role from a user.
- **Response:** `204 No Content`
- **Permissions:** `TENANT_ADMIN`

## Resource: `/permissions`

### `GET /permissions`
List all available system permissions.
- **Response:** `200 OK`, `List<PermissionResponse>`
- **Permissions:** Any authenticated user

## Resource: `/health`

### `GET /health`
Health check endpoint.
- **Response:** `200 OK`, `HealthResponse`

## Data Models

### CreateTenantRequest
```json
{
  "name": "string (required, max 255)",
  "description": "string (optional, max 1000)",
  "domain": "string (optional, max 100)",
  "settings": {
    "maxUsers": "integer (optional, default 100)",
    "defaultRoles": "array of strings (optional)",
    "customSettings": "object (optional)"
  }
}
```

### TenantResponse
```json
{
  "tenantId": "uuid",
  "name": "string",
  "description": "string",
  "domain": "string",
  "status": "ACTIVE | SUSPENDED | PENDING",
  "keycloakRealmId": "string",
  "createdAt": "datetime",
  "updatedAt": "datetime",
  "settings": {
    "maxUsers": "integer",
    "defaultRoles": "array of strings",
    "customSettings": "object"
  },
  "statistics": {
    "userCount": "integer",
    "activeUserCount": "integer",
    "roleCount": "integer"
  }
}
```

### CreateUserRequest
```json
{
  "username": "string (required, 3-50 chars)",
  "email": "string (required, valid email)",
  "firstName": "string (required, max 100)",
  "lastName": "string (required, max 100)",
  "phoneNumber": "string (optional, E.164 format)",
  "temporaryPassword": "string (optional, min 8 chars)",
  "roles": "array of strings (optional)",
  "status": "ACTIVE | INACTIVE | PENDING (optional, default ACTIVE)",
  "attributes": {
    "customAttribute": "string (optional)"
  }
}
```

### UserResponse
```json
{
  "userId": "uuid",
  "username": "string",
  "email": "string",
  "firstName": "string",
  "lastName": "string",
  "phoneNumber": "string",
  "status": "ACTIVE | INACTIVE | PENDING",
  "tenantId": "uuid",
  "roles": [
    {
      "roleId": "uuid",
      "name": "string",
      "description": "string"
    }
  ],
  "createdAt": "datetime",
  "updatedAt": "datetime",
  "lastLoginAt": "datetime",
  "attributes": {
    "customAttribute": "string"
  }
}
```

### CreateRoleRequest
```json
{
  "name": "string (required, 3-50 chars)",
  "description": "string (optional, max 500)",
  "permissions": "array of permission strings (optional)"
}
```

### RoleResponse
```json
{
  "roleId": "uuid",
  "name": "string",
  "description": "string",
  "tenantId": "uuid",
  "permissions": [
    {
      "permissionId": "string",
      "name": "string",
      "description": "string"
    }
  ],
  "userCount": "integer",
  "createdAt": "datetime",
  "updatedAt": "datetime"
}
```

### PermissionResponse
```json
{
  "permissionId": "string",
  "name": "string",
  "description": "string",
  "category": "string",
  "system": "boolean"
}
```

## Error Responses

All endpoints may return these standard error responses:

### 400 Bad Request
```json
{
  "timestamp": "datetime",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for field 'username': must not be blank",
  "path": "/v1/tenants/123/users",
  "errors": [
    {
      "field": "username",
      "message": "must not be blank",
      "rejectedValue": null
    }
  ]
}
```

### 401 Unauthorized
```json
{
  "timestamp": "datetime",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "path": "/v1/tenants"
}
```

### 403 Forbidden
```json
{
  "timestamp": "datetime",
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied: insufficient permissions",
  "path": "/v1/tenants/123/users"
}
```

### 404 Not Found
```json
{
  "timestamp": "datetime",
  "status": 404,
  "error": "Not Found",
  "message": "User not found: 456",
  "path": "/v1/tenants/123/users/456"
}
```

### 409 Conflict
```json
{
  "timestamp": "datetime",
  "status": 409,
  "error": "Conflict",
  "message": "Username already exists: john.doe",
  "path": "/v1/tenants/123/users"
}
```

### 500 Internal Server Error
```json
{
  "timestamp": "datetime",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Failed to create Keycloak realm",
  "path": "/v1/tenants"
}
```

## System Permissions

### Farm Management
- `FARM_CREATE` - Create new farms
- `FARM_READ` - View farm details
- `FARM_UPDATE` - Update farm information
- `FARM_DELETE` - Delete farms

### Plot Management
- `PLOT_CREATE` - Create new plots
- `PLOT_READ` - View plot details
- `PLOT_UPDATE` - Update plot information
- `PLOT_DELETE` - Delete plots

### User Management
- `USER_CREATE` - Create new users
- `USER_READ` - View user information
- `USER_UPDATE` - Update user information
- `USER_DELETE` - Delete users
- `USER_INVITE` - Invite users to tenant

### Role Management
- `ROLE_CREATE` - Create new roles
- `ROLE_READ` - View role information
- `ROLE_UPDATE` - Update role information
- `ROLE_DELETE` - Delete roles
- `ROLE_ASSIGN` - Assign roles to users

### Tenant Management
- `TENANT_CREATE` - Create new tenants
- `TENANT_READ` - View tenant information
- `TENANT_UPDATE` - Update tenant information
- `TENANT_DELETE` - Delete tenants

### Platform Administration
- `PLATFORM_ADMIN` - Full platform access
- `SYSTEM_MONITOR` - Access system monitoring
- `SYSTEM_CONFIG` - Modify system configuration

## Rate Limiting

API endpoints are rate-limited to prevent abuse:

- **Tenant/User Management**: 100 requests per minute per tenant
- **Authentication**: 10 requests per minute per IP
- **Health Checks**: No rate limiting

## Pagination

List endpoints support pagination using these parameters:

- `page`: Page number (default: 0)
- `size`: Page size (default: 20, max: 100)
- `sort`: Sort field and direction (e.g., `createdAt,desc`)

Response includes pagination metadata:

```json
{
  "content": [...],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "first": true,
    "last": false
  }
}
```