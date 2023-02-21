package com.sequenceiq.it.cloudbreak.testcase;

import static com.sequenceiq.it.cloudbreak.context.MeasuredTestContext.createMeasuredTestContext;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.it.cloudbreak.ResourcePropertyProvider;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.E2ETestContext;
import com.sequenceiq.it.cloudbreak.context.MeasuredTestContext;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.PurgeGarbageService;
import com.sequenceiq.it.cloudbreak.context.TestCaseDescription;
import com.sequenceiq.it.cloudbreak.context.TestCaseDescription.TestCaseDescriptionBuilder;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.exception.TestCaseDescriptionMissingException;
import com.sequenceiq.it.cloudbreak.mock.ThreadLocalProfiles;
import com.sequenceiq.it.config.AuditBeanConfig;
import com.sequenceiq.it.config.IntegrationTestConfiguration;
import com.sequenceiq.it.util.LongStringGeneratorUtil;
import com.sequenceiq.it.util.TestParameter;

@ContextConfiguration(classes = {IntegrationTestConfiguration.class, AuditBeanConfig.class},
        initializers = ConfigFileApplicationContextInitializer.class)
public abstract class AbstractMinimalTest extends AbstractTestNGSpringContextTests {

    public static final Map<String, Status> STACK_DELETED = Map.of("status", Status.DELETE_COMPLETED);

    public static final Map<String, Status> STACK_AVAILABLE = Map.of("status", Status.AVAILABLE, "clusterStatus", Status.AVAILABLE);

    public static final Map<String, Status> STACK_NODE_FAILURE = Map.of("status", Status.NODE_FAILURE, "clusterStatus", Status.NODE_FAILURE);

    protected static final Map<String, Status> STACK_CREATED = Map.of("status", Status.UPDATE_IN_PROGRESS, "clusterStatus", Status.REQUESTED);

    protected static final Map<String, Status> STACK_FAILED = Map.of("status", Status.AVAILABLE, "clusterStatus", Status.CREATE_FAILED);

    protected static final Map<String, Status> STACK_STOPPED = Map.of("status", Status.STOPPED, "clusterStatus", Status.STOPPED);

    protected static final Map<String, Status> START_IN_PROGRESS = Map.of("status", Status.START_IN_PROGRESS, "clusterStatus", Status.START_IN_PROGRESS);

    protected static final String TEST_CONTEXT_WITH_MOCK = "testContextWithMock";

    protected static final String TEST_CONTEXT = "testContextWithoutMock";

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMinimalTest.class);

    @Inject
    private ResourcePropertyProvider resourcePropertyProvider;

    @Inject
    private LongStringGeneratorUtil longStringGeneratorUtil;

    @Inject
    private CommonCloudProperties commonCloudProperties;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Inject
    private TestParameter testParameter;

    @Value("${integrationtest.cleanup.purge:false}")
    private boolean purge;

    @BeforeSuite
    public void beforeSuite(ITestContext testngContext) {
        MDC.put("testlabel", "init of " + getClass().getSimpleName());
    }

    @BeforeClass
    public void createSharedObjects() {
        if (purge) {
            String testClassName = getClass().getSimpleName();
            MDC.put("testlabel", "Purge: " + testClassName);
            applicationContext.getBean(PurgeGarbageService.class).purge();
        } else {
            LOGGER.info("Purge disabled for {}", getClass().getSimpleName());
        }
    }

    /**
     * It is invoked before the execution of each test case, where defining all the required test parameters and contexts.
     *
     * Note:
     * Test Label has been defined also as Test Parameter (as a backup solution for local test runs), because of the MDC
     * is beeing lost in case of local run in IDEA.
     * This is a special case and only occur when we are running one of the E2E test in IDEA with the IDEA Play option.
     * When the request processed by multiple threads. This means that setting the MDC context at the beginning of the
     * request is not anymore an option.
     *
     * @param method the name of the test case
     * @param params all the available test parameters
     */
    @BeforeMethod
    public void beforeTest(Method method, Object[] params) {
        LOGGER.info("Creating Test Label at Mapped Diagnostic Context and Test Parameter. " +
                "This label is used for the Cloud Storage path of the E2E tests', based on " +
                "suite and case names...");
        testParameter.put("testlabel", method.getDeclaringClass().getSimpleName() + '.' + method.getName());
        MDC.put("testlabel", method.getDeclaringClass().getSimpleName() + '.' + method.getName());
        TestContext testContext = (TestContext) params[0];
        testContext.setTestMethodName(method.getName());
        collectTestCaseDescription(testContext, method, params);
    }

    private TestCaseDescription collectTestCaseDescription(TestContext testContext, Method method, Object[] params) {
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
        LOGGER.info("Tear down context");
        ((TestContext) data[0]).cleanupTestContext();
    }

    @AfterClass(alwaysRun = true)
    public void cleanSharedObjects() {
        ThreadLocalProfiles.clearProfiles();
    }

    @DataProvider(name = TEST_CONTEXT_WITH_MOCK)
    public Object[][] testContextWithMock() {
        MockedTestContext testContext = getBean(MockedTestContext.class);
        MeasuredTestContext tc = createMeasuredTestContext(testContext);
        return new Object[][]{{tc}};
    }

    @DataProvider(name = TEST_CONTEXT)
    public Object[][] testContextWithoutMock() {
        return new Object[][]{{getBean(E2ETestContext.class)}};
    }

    public ResourcePropertyProvider resourcePropertyProvider() {
        return resourcePropertyProvider;
    }

    public LongStringGeneratorUtil getLongNameGenerator() {
        return longStringGeneratorUtil;
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

    protected CommonCloudProperties commonCloudProperties() {
        return commonCloudProperties;
    }

    protected CommonClusterManagerProperties commonClusterManagerProperties() {
        return commonClusterManagerProperties;
    }
}
