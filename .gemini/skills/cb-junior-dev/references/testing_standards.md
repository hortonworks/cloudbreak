# Cloudbreak Testing Standards

This reference documents the established testing patterns and tools for the Cloudbreak project.

## 🛠 Testing Stack

- **Framework**: JUnit 5 (`org.junit.jupiter.api.*`)
- **Mocking**: Mockito (`org.mockito.*`)
- **Assertions**: AssertJ (`org.assertj.core.api.Assertions.*`)
- **Utility**: Spring Test (`ReflectionTestUtils` for setting private fields)

## 🧪 Unit Testing Patterns

### 1. Mockito Extension
Always use `@ExtendWith(MockitoExtension.class)` for unit tests to enable `@Mock` and `@InjectMocks` annotations.

```java
@ExtendWith(MockitoExtension.class)
class MyServiceTest {
    @Mock
    private MyRepository myRepository;

    @InjectMocks
    private MyService underTest;
}
```

### 2. AssertJ for Fluid Assertions
Prefer AssertJ `assertThat()` over JUnit `assertEquals()` for better readability and more descriptive failure messages.

```java
assertThat(result).isNotNull();
assertThat(result.getStatus()).isEqualTo(Status.AVAILABLE);
```

### 3. Testing Exceptions
Use `assertThatThrownBy()` for verifying expected exceptions.

```java
assertThatThrownBy(() -> underTest.doSomething(null))
    .isInstanceOf(BadRequestException.class)
    .hasMessageContaining("cannot be null");
```

### 4. Parameterized Tests
Use `@ParameterizedTest` for testing multiple scenarios with the same logic.

```java
@ParameterizedTest
@ValueSource(strings = {"id-1", "id-2"})
void testWithIds(String id) { ... }
```

## 🏗 Directory Structure
- **Source**: `src/main/java`
- **Tests**: `src/test/java`
- **Resources**: `src/test/resources` (for JSON/YML test data)

## 🚀 Execution
Run tests for a specific module:
`./gradlew :<module>:test`
