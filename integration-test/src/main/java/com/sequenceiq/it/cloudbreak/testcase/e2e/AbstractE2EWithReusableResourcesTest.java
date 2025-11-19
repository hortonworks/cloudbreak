package com.sequenceiq.it.cloudbreak.testcase.e2e;

import static com.sequenceiq.it.util.TagsUtil.REUSABLE_RESOURCE_TAG_PREFIX;

import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;

import com.sequenceiq.cloudbreak.util.BouncyCastleFipsProviderLoader;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.config.testinformation.TestInformation;
import com.sequenceiq.it.cloudbreak.config.testinformation.TestInformationService;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractSdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.util.spot.SpotUtil;

public abstract class AbstractE2EWithReusableResourcesTest extends AbstractE2ETest {

    protected static String environmentName = "";

    protected static String datalakeName = "";

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractE2EWithReusableResourcesTest.class);

    private static final Set<TestContext> TEST_CONTEXTS = new java.util.concurrent.CopyOnWriteArraySet<>();

    @Inject
    private SpotUtil spotUtil;

    @Inject
    private TestInformationService testInformationService;

    @Inject
    private SdxTestClient sdxTestClient;

    /**
     * Runs once before all tests in the class to set up reusable environment and datalake.
     */
    @BeforeClass
    public final void minimalSetupClass() {
        BouncyCastleFipsProviderLoader.load();
        TestContext testContext = getBean(TestContext.class);
        String className = getClass().getSimpleName();
        LOGGER.info("Running minimalSetupClass in: {}", className);
        testContext.setTestMethodName(REUSABLE_RESOURCE_TAG_PREFIX + "-" + className);
        testInformationService.setTestInformation(new TestInformation(className, className));

        setupClass(testContext);

        environmentName = testContext.get(EnvironmentTestDto.class).getName();
        datalakeName = testContext.getInstanceOf(AbstractSdxTestDto.class).getName();
        TEST_CONTEXTS.add(testContext);
    }

    /**
     * Override this to add class level setup, it will run once before all tests in the class.
     */
    protected void setupClass(TestContext testContext) {
    }

    /**
     * Runs before each test to reuse the environment and datalake.
     */
    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        useExistingEnvironment(testContext, environmentName);
        useExistingDatalake(testContext, datalakeName);
    }

    /**
     * Runs after each test to collect test contexts for later cleanup.
     */
    @Override
    @AfterMethod(alwaysRun = true)
    public void tearDown(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        TEST_CONTEXTS.add(testContext);
    }

    /**
     * Runs once after all tests in the class to clean up test contexts.
     */
    @AfterClass(alwaysRun = true)
    protected void cleanupTestContextAfterClass() {
        MDC.put("testlabel", null);
        testInformationService.removeTestInformation();
        LOGGER.info("Tear down contexts");
        for (TestContext testContext : TEST_CONTEXTS) {
            LOGGER.info("Cleaning up test context for test method: {}", testContext.getTestMethodName().orElse("unknown"));
            testContext.cleanupTestContext();
        }
    }
}
