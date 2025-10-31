# Contributing to Agri Enhancement Platform

Thank you for your interest in contributing to the Agri Enhancement Platform (AEP)! This guide will help you understand how to contribute effectively.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Coding Standards](#coding-standards)
- [Testing Guidelines](#testing-guidelines)
- [Documentation](#documentation)
- [Pull Request Process](#pull-request-process)
- [Review Process](#review-process)

## Code of Conduct

We are committed to providing a welcoming and inclusive environment for all contributors. Please:

- Be respectful and considerate
- Use inclusive language
- Focus on constructive feedback
- Help others learn and grow

## Getting Started

### Prerequisites

Before contributing, ensure you have:

- Read the [Getting Started Guide](GETTING_STARTED.md)
- Set up your local development environment
- Familiarized yourself with the project structure

### Choose an Issue

1. Browse [open issues](../../issues) for good first contributions
2. Look for issues labeled `good first issue` or `help wanted`
3. Comment on the issue to express your interest
4. Wait for assignment before starting work

## Development Workflow

### 1. Fork and Clone

```bash
# Fork the repository on GitHub
git clone https://github.com/your-username/agri-enhancement-platform.git
cd agri-enhancement-platform

# Add upstream remote
git remote add upstream https://github.com/original-owner/agri-enhancement-platform.git
```

### 2. Create a Feature Branch

```bash
# Sync with upstream main
git fetch upstream
git checkout main
git merge upstream/main

# Create feature branch
git checkout -b feature/your-feature-name
```

### 3. Make Changes

- Follow the [Coding Standards](#coding-standards)
- Write [tests](#testing-guidelines) for your changes
- Update [documentation](#documentation) as needed

### 4. Commit Changes

Follow our commit message format:

```
type(scope): description

[optional body]

[optional footer]
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

**Examples:**
```
feat(farm-plot): add plot geometry validation

Add validation for plot polygons to ensure they are valid
geometries before saving to database.

Fixes #123
```

```
fix(iam): resolve tenant isolation issue

Users were able to access data from other tenants due to
missing tenant context validation in service layer.

Closes #456
```

### 5. Push and Create Pull Request

```bash
# Push to your fork
git push origin feature/your-feature-name

# Create pull request on GitHub
```

## Coding Standards

### Java Code Style

We follow standard Java conventions with these specific guidelines:

#### 1. Package Structure

```
com.agrienhance.<service-name>/
├── api/          # Controllers, DTOs
├── application/  # Services, application logic
├── domain/       # Entities, repositories, domain logic
└── infrastructure/ # External integrations, configuration
```

#### 2. Class Naming

- **Classes**: PascalCase (e.g., `FarmPlotService`)
- **Methods**: camelCase (e.g., `createFarm`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_PLOT_SIZE`)
- **Variables**: camelCase (e.g., `farmIdentifier`)

#### 3. Code Formatting

- Use 4 spaces for indentation (no tabs)
- Maximum line length: 120 characters
- One statement per line
- Use meaningful variable names

#### 4. Documentation

```java
/**
 * Service for managing farm and plot operations.
 * 
 * @author Your Name
 * @since 1.0.0
 */
@Service
public class FarmPlotService {
    
    /**
     * Creates a new farm with the given details.
     * 
     * @param request the farm creation request
     * @return the created farm response
     * @throws ValidationException if request is invalid
     */
    public FarmResponse createFarm(CreateFarmRequest request) {
        // implementation
    }
}
```

#### 5. Exception Handling

```java
// Use specific exception types
throw new EntityNotFoundException("Farm not found: " + farmId);

// Use @Valid for request validation
@PostMapping("/farms")
public ResponseEntity<FarmResponse> createFarm(@Valid @RequestBody CreateFarmRequest request) {
    // implementation
}
```

### Spring Boot Specific Guidelines

#### 1. Configuration Properties

```java
@ConfigurationProperties(prefix = "aep.farm-plot")
@Data
public class FarmPlotProperties {
    private int maxPlotsPerFarm = 100;
    private boolean enableGeometryValidation = true;
}
```

#### 2. Repository Naming

```java
// Use descriptive method names
@Repository
public interface FarmRepository extends JpaRepository<Farm, UUID> {
    
    List<Farm> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    
    @Query("SELECT f FROM Farm f WHERE f.ownerReferenceId = :ownerId")
    Page<Farm> findByOwner(@Param("ownerId") UUID ownerId, Pageable pageable);
}
```

#### 3. Service Layer

```java
@Service
@Transactional
@Slf4j
public class FarmPlotService {
    
    private final FarmRepository farmRepository;
    private final FarmMapper farmMapper;
    
    public FarmPlotService(FarmRepository farmRepository, FarmMapper farmMapper) {
        this.farmRepository = farmRepository;
        this.farmMapper = farmMapper;
    }
    
    // Methods should be transactional and include proper logging
}
```

## Testing Guidelines

### Test Structure

```
src/test/java/com/agrienhance/<service-name>/
├── api/          # Controller tests
├── application/  # Service tests
├── domain/       # Repository and entity tests
└── integration/  # Integration tests
```

### Unit Tests

#### 1. Service Tests

```java
@ExtendWith(MockitoExtension.class)
class FarmPlotServiceTest {
    
    @Mock
    private FarmRepository farmRepository;
    
    @Mock
    private FarmMapper farmMapper;
    
    @InjectMocks
    private FarmPlotService farmPlotService;
    
    @Test
    @DisplayName("Should create farm successfully when request is valid")
    void createFarm_ValidRequest_ReturnsFarmResponse() {
        // Given
        CreateFarmRequest request = CreateFarmRequest.builder()
            .farmName("Test Farm")
            .ownerReferenceId(UUID.randomUUID())
            .countryCode("US")
            .build();
            
        Farm savedFarm = Farm.builder()
            .farmIdentifier(UUID.randomUUID())
            .farmName(request.getFarmName())
            .build();
            
        when(farmRepository.save(any(Farm.class))).thenReturn(savedFarm);
        
        // When
        FarmResponse response = farmPlotService.createFarm(request);
        
        // Then
        assertThat(response.getFarmName()).isEqualTo(request.getFarmName());
        verify(farmRepository).save(any(Farm.class));
    }
}
```

#### 2. Controller Tests

```java
@WebMvcTest(FarmController.class)
class FarmControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private FarmPlotService farmPlotService;
    
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
            
        when(farmPlotService.createFarm(any())).thenReturn(response);
        
        // When & Then
        mockMvc.perform(post("/v1/farms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.farmName").value("Test Farm"));
    }
}
```

### Integration Tests

```java
@SpringBootTest
@Testcontainers
@Transactional
class FarmPlotServiceIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgis/postgis:15-3.3")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    @Autowired
    private FarmPlotService farmPlotService;
    
    @Autowired
    private FarmRepository farmRepository;
    
    @Test
    @DisplayName("Should persist farm in database")
    void createFarm_ValidRequest_PersistsFarm() {
        // Given
        CreateFarmRequest request = CreateFarmRequest.builder()
            .farmName("Integration Test Farm")
            .ownerReferenceId(UUID.randomUUID())
            .countryCode("US")
            .build();
        
        // When
        FarmResponse response = farmPlotService.createFarm(request);
        
        // Then
        assertThat(response.getFarmIdentifier()).isNotNull();
        
        Optional<Farm> savedFarm = farmRepository.findById(response.getFarmIdentifier());
        assertThat(savedFarm).isPresent();
        assertThat(savedFarm.get().getFarmName()).isEqualTo("Integration Test Farm");
    }
}
```

### Test Coverage

- Aim for >80% code coverage
- Write tests for all public methods
- Test both positive and negative scenarios
- Include edge cases and error conditions

## Documentation

### Code Documentation

- Add JavaDoc for all public APIs
- Document complex business logic
- Include examples in documentation

### README Updates

- Update feature lists
- Add new configuration options
- Include breaking changes

### API Documentation

- Update OpenAPI specifications
- Document new endpoints
- Include request/response examples

## Pull Request Process

### Before Submitting

1. **Code Quality**
   - Run full test suite: `mvn verify`
   - Check code formatting
   - Ensure no compilation warnings

2. **Documentation**
   - Update relevant documentation
   - Add comments to complex code
   - Update API documentation if needed

3. **Testing**
   - All tests must pass
   - Add new tests for new functionality
   - Maintain code coverage

### Pull Request Template

```markdown
## Description
Brief description of changes made.

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual testing completed

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] Tests added/updated

## Issues
Closes #issue_number
```

### PR Requirements

1. **Clear Title and Description**
   - Use conventional commit format for title
   - Describe what was changed and why
   - Include screenshots if applicable

2. **Small, Focused Changes**
   - One feature or fix per PR
   - Keep PRs manageable in size
   - Split large changes into multiple PRs

3. **Testing**
   - All tests must pass
   - New features must include tests
   - Bug fixes must include regression tests

## Review Process

### Reviewer Responsibilities

1. **Code Quality**
   - Check adherence to coding standards
   - Verify proper error handling
   - Assess performance implications

2. **Functionality**
   - Verify requirements are met
   - Check for edge cases
   - Validate business logic

3. **Testing**
   - Review test coverage
   - Check test quality
   - Verify test assertions

### Author Responsibilities

1. **Address Feedback**
   - Respond to all comments
   - Make requested changes
   - Explain decisions when disagreeing

2. **Update PR**
   - Keep PR description current
   - Add new commits for changes
   - Resolve merge conflicts

## Getting Help

If you need help:

1. Check existing [documentation](Docs/)
2. Search [existing issues](../../issues)
3. Ask questions in [discussions](../../discussions)
4. Contact maintainers

## Recognition

Contributors are recognized in:

- Release notes
- Contributor documentation
- Project acknowledgments

Thank you for contributing to AEP! 🚜
