package com.sequenceiq.it.cloudbreak.testcase.mock;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.BeansException;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.it.cloudbreak.ResourcePropertyProvider;
import com.sequenceiq.it.cloudbreak.action.v4.imagecatalog.ImageCatalogCreateRetryAction;
import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.config.user.TestUserCreator;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestCaseDescription;
import com.sequenceiq.it.cloudbreak.context.TestCaseDescription.TestCaseDescriptionBuilder;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestCaseDescriptionMissingException;
import com.sequenceiq.it.config.IntegrationTestConfiguration;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@ContextConfiguration(classes = {IntegrationTestConfiguration.class},
        initializers = ConfigDataApplicationContextInitializer.class)
public class MultiThreadTenantTest extends AbstractTestNGSpringContextTests {

    public static final Map<String, Status> STACK_DELETED = Map.of("status", Status.DELETE_COMPLETED);

    protected static final Map<String, Status> STACK_AVAILABLE = Map.of("status", Status.AVAILABLE, "clusterStatus", Status.AVAILABLE);

    protected static final Map<String, Status> STACK_FAILED = Map.of("status", Status.AVAILABLE, "clusterStatus", Status.CREATE_FAILED);

    protected static final Map<String, Status> STACK_STOPPED = Map.of("status", Status.STOPPED, "clusterStatus", Status.STOPPED);

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiThreadTenantTest.class);

    private static final String TEST_CONTEXT_THREAD = "testContextThread";

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private ImageCatalogTestClient imageCatalogTestClient;

    @Inject
    private BlueprintTestClient blueprintTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private ResourcePropertyProvider resourcePropertyProvider;

    @Inject
    private TestUserCreator testUserCreator;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @BeforeMethod
    public void beforeTest(Method method, Object[] params) {
        MDC.put("testlabel", method.getDeclaringClass().getSimpleName() + '.' + method.getName());
        MockedTestContext testContext = (MockedTestContext) params[0];
        testContext.setTestMethodName(method.getName());
        collectTestCaseDescription(testContext, method, params);
    }

    protected TestContext prepareTestContext(MockedTestContext testContext, String tenant, String user) {
        createAdminUser(testContext, tenant, user);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
        return testContext;
    }

    private TestCaseDescription collectTestCaseDescription(MockedTestContext testContext, Method method, Object[] params) {
        Description declaredAnnotation = method.getDeclaredAnnotation(Description.class);
        TestCaseDescription testCaseDescription = null;
        if (declaredAnnotation != null) {
            testCaseDescription = new TestCaseDescriptionBuilder()
                    .given(declaredAnnotation.given())
                    .when(declaredAnnotation.when())
                    .then(declaredAnnotation.then());
            testContext.addDescription(testCaseDescription);
        } else if (method.getParameters().length == params.length) {
            Parameter[] parameters = method.getParameters();
            for (int i = 1; i < parameters.length; i++) {
                if (parameters[i].getAnnotation(Description.class) != null) {
                    Object param = params[i];
                    if (!(param instanceof TestCaseDescription)) {
                        throw new IllegalArgumentException("The param annotated with @Description but the type is should be "
                                + TestCaseDescription.class.getSimpleName());
                    }
                    testCaseDescription = (TestCaseDescription) param;
                    testContext.addDescription(testCaseDescription);
                    break;
                }
            }
        }
        return Optional.ofNullable(testCaseDescription)
                .filter(d -> !Strings.isNullOrEmpty(d.getValue()))
                .orElseThrow(() -> new TestCaseDescriptionMissingException(method.getName()));
    }

    @AfterMethod
    public void afterMethod(Method method, ITestResult testResult) {
        MDC.put("testlabel", null);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(Object[] data) {
        ((MockedTestContext) data[0]).cleanupTestContext();
    }

    @DataProvider(name = TEST_CONTEXT_THREAD, parallel = true)
    public Object[][] testContextWithMock() {
        return new Object[][]{
                {prepareTestContext(getBean(MockedTestContext.class), "hw", "userhw")},
                {prepareTestContext(getBean(MockedTestContext.class), "bm", "userbm")},
                {prepareTestContext(getBean(MockedTestContext.class), "pm", "userpm")},
                {prepareTestContext(getBean(MockedTestContext.class), "pm1", "userpm")},
        };
    }

    protected void createDefaultCredential(MockedTestContext testContext) {
        testContext.given(CredentialTestDto.class)
                .when(credentialTestClient.create());
    }

    protected void createDefaultImageCatalog(MockedTestContext testContext) {
        testContext
                .given(ImageCatalogTestDto.class)
                .when(new ImageCatalogCreateRetryAction());
    }

    protected void createAdminUser(MockedTestContext testContext, String tenant, String user) {
        testContext.as(testUserCreator.createAdmin(tenant, user));
    }

    protected void initializeDefaultBlueprints(MockedTestContext testContext) {
        testContext
                .init(BlueprintTestDto.class)
                .when(blueprintTestClient.listV4());
    }

    /**
     * Obtains bean from the application context for the given type if both the bean and the application context exists
     *
     * @param requiredType the class of the expected bean
     * @param <T>          generic for the type of the expected bean
     * @return extracted instance from the application context
     * @throws IllegalStateException if no application context exists or bean could not be created
     */
    protected <T> T getBean(Class<T> requiredType) {
        LOGGER.info("Getting for test bean: {}", requiredType.getName());
        if (applicationContext != null) {
            try {
                T bean = applicationContext.getBean(requiredType);
                LOGGER.info("bean created. ref: {}", bean);
                return bean;
            } catch (BeansException be) {
                throw new IllegalStateException("No bean found!", be);
            }
        }
        throw new IllegalStateException("No application context found!");
    }

    @Test(dataProvider = TEST_CONTEXT_THREAD)
    @Description(
            given = "there is a running cloudbreak",
            when = "create an attached SDX and Datahub",
            then = "should not get 404 errors due to thread safe operations")
    public void testParallelMultiTenantStacks(MockedTestContext testContext) {
        testContext
                .given(EnvironmentNetworkTestDto.class)
                .given(EnvironmentTestDto.class).withNetwork()
                .when(environmentTestClient.create())
                .awaitForCreationFlow()
                .given(FreeIpaTestDto.class).withEnvironment()
                .when(freeIpaTestClient.create())
                .awaitForCreationFlow()
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal())
                .await(SdxClusterStatusResponse.RUNNING)
                .given(DistroXTestDto.class)
                .when(distroXTestClient.create())
                .await(STACK_AVAILABLE)
                .validate();
    }
}
