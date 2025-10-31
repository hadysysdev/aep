# Testing Guidelines

This document outlines the testing strategy, standards, and best practices for the Agri Enhancement Platform (AEP).

## Testing Strategy Overview

AEP follows the Testing Pyramid approach with three main layers:

```
    /\
   /  \  E2E Tests (Few)
  /____\
 /      \ Integration Tests (Some)
/________\
Unit Tests (Many)
```

## 1. Unit Tests

### Purpose
- Test individual components in isolation
- Fast feedback loop for developers
- Validate business logic at the smallest level

### Guidelines

#### Test Structure
```java
@ExtendWith(MockitoExtension.class)
class FarmServiceTest {
    
    @Mock
    private FarmRepository farmRepository;
    
    @Mock
    private FarmMapper farmMapper;
    
    @InjectMocks
    private FarmService farmService;
    
    @Test
    @DisplayName("Should create farm when valid request provided")
    void createFarm_ValidRequest_ReturnsFarmResponse() {
        // Test implementation
    }
}
```

#### Naming Conventions
- **Test Class**: `{ClassName}Test`
- **Test Method**: `methodName_condition_expectedResult`
- **Display Names**: Descriptive, human-readable names

#### Best Practices
1. **Arrange, Act, Assert (AAA) Pattern**
```java
@Test
void createFarm_ValidRequest_ReturnsFarmResponse() {
    // Arrange
    CreateFarmRequest request = buildCreateFarmRequest();
    Farm savedFarm = buildSavedFarm();
    FarmResponse expectedResponse = buildFarmResponse();
    
    when(farmRepository.save(any(Farm.class))).thenReturn(savedFarm);
    when(farmMapper.toResponse(savedFarm)).thenReturn(expectedResponse);
    
    // Act
    FarmResponse actualResponse = farmService.createFarm(request);
    
    // Assert
    assertThat(actualResponse).isEqualTo(expectedResponse);
    verify(farmRepository).save(any(Farm.class));
}
```

2. **Use Meaningful Test Data**
```java
private CreateFarmRequest buildCreateFarmRequest() {
    return CreateFarmRequest.builder()
        .farmName("Sunrise Farm")
        .ownerReferenceId(UUID.randomUUID())
        .countryCode("KE")
        .region("Rift Valley")
        .build();
}
```

3. **Test Both Positive and Negative Cases**
```java
@Test
void createFarm_NullFarmName_ThrowsValidationException() {
    // Arrange
    CreateFarmRequest request = CreateFarmRequest.builder()
        .farmName(null) // Invalid
        .ownerReferenceId(UUID.randomUUID())
        .countryCode("KE")
        .build();
    
    // Act & Assert
    assertThatThrownBy(() -> farmService.createFarm(request))
        .isInstanceOf(ValidationException.class)
        .hasMessage("Farm name is required");
}
```

#### Coverage Requirements
- **Minimum Coverage**: 80% line coverage
- **Target Coverage**: 90% line coverage
- **Critical Path**: 100% coverage for business-critical methods

## 2. Integration Tests

### Purpose
- Test interaction between components
- Validate database operations
- Test external service integrations

### Guidelines

#### Test Structure
```java
@SpringBootTest
@Testcontainers
@Transactional
class FarmServiceIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgis/postgis:15-3.3")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    @Autowired
    private FarmService farmService;
    
    @Autowired
    private FarmRepository farmRepository;
    
    @Test
    @DisplayName("Should persist farm in database")
    void createFarm_ValidRequest_PersistsFarm() {
        // Test implementation
    }
}
```

#### Database Testing
```java
@Test
void createFarm_ValidRequest_PersistsFarm() {
    // Given
    CreateFarmRequest request = CreateFarmRequest.builder()
        .farmName("Test Farm")
        .ownerReferenceId(UUID.randomUUID())
        .countryCode("KE")
        .build();
    
    // When
    FarmResponse response = farmService.createFarm(request);
    
    // Then
    assertThat(response.getFarmIdentifier()).isNotNull();
    
    Optional<Farm> savedFarm = farmRepository.findById(response.getFarmIdentifier());
    assertThat(savedFarm).isPresent();
    assertThat(savedFarm.get().getFarmName()).isEqualTo("Test Farm");
    assertThat(savedFarm.get().getCountryCode()).isEqualTo("KE");
}
```

#### External Service Testing
```java
@SpringBootTest
@Testcontainers
class IdentityServiceIntegrationTest {
    
    @Container
    static GenericContainer<?> keycloak = new GenericContainer<>("quay.io/keycloak/keycloak:25.0.2")
            .withExposedPorts(8080)
            .withEnv("KEYCLOAK_ADMIN", "admin")
            .withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin");
    
    @Test
    void validateUser_ValidUser_ReturnsUserValidationResponse() {
        // Set up Keycloak realm and user
        setupKeycloakRealm();
        
        // Test user validation through Keycloak
        UserValidationResponse response = identityService.validateUser(userId);
        
        assertThat(response.isValid()).isTrue();
        assertThat(response.getTenantId()).isNotNull();
    }
}
```

## 3. End-to-End Tests

### Purpose
- Test complete user workflows
- Validate system behavior from user perspective
- Test integration between all services

### Guidelines

#### Test Structure
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class FarmManagementE2ETest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @LocalServerPort
    private int port;
    
    @Test
    @DisplayName("Should create farm and verify in database")
    void createFarmWorkflow_CompletesSuccessfully() {
        // Create user through IAM service
        UUID userId = createTestUser();
        
        // Create farm through API
        CreateFarmRequest request = buildCreateFarmRequest(userId);
        ResponseEntity<FarmResponse> createResponse = restTemplate.postForEntity(
            getBaseUrl() + "/v1/farms", 
            request, 
            FarmResponse.class
        );
        
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        
        // Verify farm exists
        UUID farmId = createResponse.getBody().getFarmIdentifier();
        ResponseEntity<FarmResponse> getResponse = restTemplate.getForEntity(
            getBaseUrl() + "/v1/farms/" + farmId,
            FarmResponse.class
        );
        
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().getFarmName()).isEqualTo(request.getFarmName());
    }
}
```

## 4. API Testing

### Controller Tests
```java
@WebMvcTest(FarmController.class)
class FarmControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private FarmService farmService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @DisplayName("Should return 201 when creating farm")
    void createFarm_ValidRequest_ReturnsCreated() throws Exception {
        // Given
        CreateFarmRequest request = CreateFarmRequest.builder()
            .farmName("Test Farm")
            .build();
            
        FarmResponse response = FarmResponse.builder()
            .farmIdentifier(UUID.randomUUID())
            .farmName("Test Farm")
            .build();
            
        when(farmService.createFarm(any())).thenReturn(response);
        
        // When & Then
        mockMvc.perform(post("/v1/farms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.farmIdentifier").exists())
                .andExpect(jsonPath("$.farmName").value("Test Farm"))
                .andExpect(header().string("Location", containsString("/v1/farms/")));
    }
    
    @Test
    @DisplayName("Should return 400 when request is invalid")
    void createFarm_InvalidRequest_ReturnsBadRequest() throws Exception {
        // Given
        CreateFarmRequest request = CreateFarmRequest.builder()
            .farmName("") // Invalid
            .build();
        
        // When & Then
        mockMvc.perform(post("/v1/farms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }
}
```

## 5. Test Data Management

### Testcontainers Configuration
```java
@TestConfiguration
public class TestContainerConfig {
    
    @Bean
    @Primary
    public PostgreSQLContainer<?> postgreSQLContainer() {
        return new PostgreSQLContainer<>("postgis/postgis:15-3.3")
                .withDatabaseName("aep_test")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true);
    }
    
    @Bean
    @Primary
    public DataSource dataSource(PostgreSQLContainer<?> container) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(container.getJdbcUrl());
        config.setUsername(container.getUsername());
        config.setPassword(container.getPassword());
        return new HikariDataSource(config);
    }
}
```

### Test Data Builders
```java
public class FarmTestDataBuilder {
    
    public static Farm createFarm() {
        return Farm.builder()
            .farmIdentifier(UUID.randomUUID())
            .farmName("Test Farm")
            .ownerReferenceId(UUID.randomUUID())
            .countryCode("KE")
            .region("Rift Valley")
            .tenantId(UUID.randomUUID())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .version(0L)
            .build();
    }
    
    public static CreateFarmRequest createFarmRequest() {
        return CreateFarmRequest.builder()
            .farmName("Test Farm")
            .ownerReferenceId(UUID.randomUUID())
            .countryCode("KE")
            .region("Rift Valley")
            .build();
    }
    
    public static FarmResponse farmResponse() {
        return FarmResponse.builder()
            .farmIdentifier(UUID.randomUUID())
            .farmName("Test Farm")
            .countryCode("KE")
            .region("Rift Valley")
            .build();
    }
}
```

## 6. Performance Testing

### Load Testing
```java
@SpringBootTest
class FarmServicePerformanceTest {
    
    @Autowired
    private FarmService farmService;
    
    @Test
    @DisplayName("Should handle concurrent farm creation")
    void createFarms_ConcurrentRequests_HandlesLoad() throws InterruptedException {
        int threadCount = 10;
        int requestsPerThread = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        CreateFarmRequest request = FarmTestDataBuilder.createFarmRequest();
                        farmService.createFarm(request);
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        latch.await(30, TimeUnit.SECONDS);
        
        assertThat(successCount.get()).isEqualTo(threadCount * requestsPerThread);
    }
}
```

## 7. Security Testing

### Authentication Tests
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    @DisplayName("Should deny access without authentication")
    void accessEndpoint_WithoutAuthentication_Returns401() {
        ResponseEntity<String> response = restTemplate.getForEntity("/v1/farms", String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
    
    @Test
    @DisplayName("Should allow access with valid JWT")
    void accessEndpoint_WithValidJwt_Returns200() {
        String jwtToken = obtainValidJwtToken();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
            "/v1/farms", 
            HttpMethod.GET, 
            entity, 
            String.class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
```

## 8. Test Configuration

### Application Test Properties
```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:tc:postgresql:15-3.3:///aep_test
    username: test
    password: test
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  flyway:
    enabled: true
    
keycloak:
  server-url: http://localhost:8080
  realm: test-realm

logging:
  level:
    com.agrienhance: DEBUG
    org.springframework.security: DEBUG
```

### Test Profiles
```java
@ActiveProfiles("test")
@SpringBootTest
class AbstractIntegrationTest {
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("keycloak.server-url", () -> "http://" + keycloak.getHost() + ":" + keycloak.getFirstMappedPort());
    }
}
```

## 9. Continuous Integration

### Maven Surefire Configuration
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <includes>
            <include>**/*Test.java</include>
            <include>**/*Tests.java</include>
        </includes>
        <excludes>
            <exclude>**/*IT.java</exclude>
            <exclude>**/*E2ETest.java</exclude>
        </excludes>
        <systemPropertyVariables>
            <spring.profiles.active>test</spring.profiles.active>
        </systemPropertyVariables>
    </configuration>
</plugin>
```

### Maven Failsafe Configuration
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <configuration>
        <includes>
            <include>**/*IT.java</include>
            <include>**/*E2ETest.java</include>
        </includes>
        <systemPropertyVariables>
            <spring.profiles.active>integration-test</spring.profiles.active>
        </systemPropertyVariables>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>integration-test</goal>
                <goal>verify</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

## 10. Best Practices Summary

### Do's
- Write tests before fixing bugs (TDD approach)
- Use descriptive test names
- Test edge cases and error conditions
- Keep tests independent and isolated
- Use test data builders for consistent test data
- Mock external dependencies appropriately
- Maintain high test coverage

### Don'ts
- Don't test framework code (e.g., Spring functionality)
- Don't make tests dependent on execution order
- Don't use real databases in unit tests
- Don't ignore failing tests
- Don't write tests that are too complex
- Don't test multiple concerns in one test

## 11. Reporting and Metrics

### JaCoCo Configuration
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <configuration>
        <excludes>
            <exclude>**/config/**</exclude>
            <exclude>**/dto/**</exclude>
            <exclude>**/entity/**</exclude>
        </excludes>
        <rules>
            <rule>
                <element>BUNDLE</element>
                <limits>
                    <limit>
                        <counter>LINE</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.80</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</plugin>
```

### Test Reports
- Unit test results: `target/surefire-reports/`
- Integration test results: `target/failsafe-reports/`
- Coverage report: `target/site/jacoco/index.html`

These testing guidelines ensure comprehensive test coverage, maintainable test code, and reliable quality assurance for the AEP platform.