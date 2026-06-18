---
name: cb-testing
description: Write and run tests in Cloudbreak — JUnit 5 + Mockito unit tests, Spring integration tests, the mandatory authorization-annotation compliance test, flow-chain graph tests, plus Gradle/jacoco commands and coverage gates. Use when adding tests, reproducing a bug test-first, or before opening a PR.
---

# Cloudbreak Testing

Java 21, Spring Boot 3.3, Gradle, **JUnit 5 (Jupiter) + Mockito**, **AssertJ** as the primary assertion library. New logic needs a unit test — it's frequently the sole blocker on an otherwise-approved PR. For the reproduction-first bug workflow see `.agent/WORKFLOW.md`.

## Layout & naming

All tests live in `src/test/java/` (no separate source set). The suffix signals the kind:

| Suffix | Kind | Notes |
|---|---|---|
| `*Test` | Unit test | `@ExtendWith(MockitoExtension.class)`, pure mocks, fast |
| `*IntegrationTest` | Spring integration test | `@ActiveProfiles("integration-test")`, real wiring |
| `*ComponentTest` | Component test | Testcontainers (e.g. Postgres); excluded from the default `test` run unless `-PcomponentTest` |
| `*TestBase` | Shared abstract base | not a runnable test |

Mirror the production package. Reusable fixtures go in a module `TestUtil` / builder classes — reuse them instead of hand-building entities.

## Running tests

```bash
./gradlew :core:test                                            # one module
./gradlew :cluster-cm:test --tests ClouderaManagerKerberosServiceTest        # one class
./gradlew :cluster-cm:test --tests ClouderaManagerKerberosServiceTest.testConfigureKerberosViaApi  # one method
./gradlew test jacocoTestReport                                 # all + coverage
```

CI (`.github/workflows/pull-request.yaml`) runs `test` + `jacocoTestReport` with checkstyle/spotbugs skipped, `--parallel --no-daemon`. Results: `**/build/test-results/test/TEST-*.xml`; coverage XML: `build/reports/jacoco/test/jacocoTestReport.xml`.

## Unit test idiom

```java
@ExtendWith(MockitoExtension.class)
class FooServiceTest {
    @Mock private BarRepository barRepository;
    @InjectMocks private FooService underTest;

    @Test
    void returnsBarWhenPresent() {
        when(barRepository.findByCrn("crn")).thenReturn(Optional.of(bar));
        assertThat(underTest.get("crn")).isEqualTo(bar);   // AssertJ
        verify(barRepository).findByCrn("crn");
    }
}
```

- `@Mock` + `@InjectMocks`; `@BeforeEach` for setup. Prefer AssertJ `assertThat(...)`; use `assertThrows(...)` for exceptions.
- Test both branches of new conditionals (null/non-null, empty/non-empty) — reviewers ask for the missing branch explicitly.

## Spring integration test idiom

```java
@ActiveProfiles("integration-test")
@ExtendWith(SpringExtension.class)
class FooFlowIntegrationTest {
    @Inject private ReactorNotifier reactorNotifier;
    @MockBean private ExternalClient externalClient;   // mock the boundary
    @SpyBean private TransactionalScheduler scheduler;

    @Profile("integration-test")
    @TestConfiguration
    @Import({ FooActions.class, FooFlowConfig.class, /* handlers... */ })
    static class TestConfig { }
}
```

`@MockBean`/`@SpyBean` for Spring-managed mocks; a nested `@TestConfiguration` imports only the beans under test. `@TestPropertySource(properties = {...})` overrides config. Testcontainers (`@Testcontainers` + `@Container PostgreSQLContainer`) backs component tests that need a real DB.

> Most existing tests use `@MockBean`/`@SpyBean`, but Spring has deprecated them — **new tests should prefer `@MockitoBean`/`@MockitoSpyBean`** (`org.springframework.test.context.bean.override.mockito`).

For driving a flow's state machine in an integration test (trigger via `flowManager.notify(...)`, poll `flowRegister` to wait, assert with `InOrder`), see **cb-flow-engine**.

## Mandatory: authorization compliance test

Every module that has controllers and imports `authorization-common` MUST ship an `EnforceAuthorizationAnnotationsTest` (see `core`, `redbeams`, `externalized-compute`). It delegates to `EnforceAuthorizationAnnotationTestUtil` and fails the build if any controller/method lacks proper authorization annotations or a required resource provider:

```java
public class EnforceAuthorizationAnnotationsTest {
    @Test void testIfControllerClassHasProperAnnotation() {
        EnforceAuthorizationAnnotationTestUtil.testIfControllerClassHasProperAnnotation();
    }
    @Test void testIfControllerMethodsHaveProperAuthorizationAnnotation() {
        EnforceAuthorizationAnnotationTestUtil.testIfControllerMethodsHaveProperAuthorizationAnnotation();
    }
    @Test void testIfAllNecessaryResourceProviderPresent() {
        EnforcePropertyProviderTestUtil.testIfAllNecessaryResourceProviderImplemented();
    }
}
```

Adding a new controller/endpoint? This test already guards it — run the module's `test` to confirm your authorization annotations satisfy it (see **cb-flow-engine** for the auth dimension and `authorization-common/AGENTS.md`).

## Flow tests

- **Chain wiring:** in `*FlowEventChainFactoryTest`, `FlowChainConfigGraphGeneratorUtil.generateFor(underTest, "com.sequenceiq.flow.config", queue, "SCENARIO")` validates the chain config (see `UpscaleFlowEventChainFactoryTest`).
- **Whole flow:** drive the real state machine in a `*FlowIntegrationTest` (pattern above). See **cb-flow-engine**.

## Coverage gate

Jacoco, enforced in CI at **55% overall and 55% on changed files** (`.github/actions/pull-request/unit-test/action.yaml`); SonarQube ingests the reports. Generated/proto code is excluded. Keep changed-file coverage above the line or the PR check fails — another reason new logic ships with its test.
