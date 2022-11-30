package com.sequenceiq.it.cloudbreak.testcase.e2e;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.not;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Listeners;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.cloudbreak.util.spot.SpotRetryOnceTestListener;
import com.sequenceiq.it.cloudbreak.util.spot.SpotRetryUtil;
import com.sequenceiq.it.cloudbreak.util.spot.SpotUtil;
import com.sequenceiq.it.util.TagsUtil;

@Listeners(SpotRetryOnceTestListener.class)
public abstract class AbstractE2ETest extends AbstractIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractE2ETest.class);

    @Inject
    private SpotUtil spotUtil;

    @Inject
    private SpotRetryUtil spotRetryUtil;

    @Inject
    private TagsUtil tagsUtil;

    @Override
    protected void setupTest(ITestResult testResult) {
        boolean shouldUseSpotInstances = spotUtil.shouldUseSpotInstancesForTest(testResult.getMethod().getConstructorOrMethod().getMethod());
        boolean retried = spotRetryUtil.isRetried(testResult.getMethod());
        spotUtil.setUseSpotInstances(shouldUseSpotInstances && !retried);
    }

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultEnvironment(testContext);
    }

    @AfterMethod
    public void tearDownSpotValidateTags(Object[] data) {
        if (MapUtils.isEmpty(((TestContext) data[0]).getExceptionMap())) {
            LOGGER.info("Validating default tags on the created and tagged test resources...");
            ((TestContext) data[0]).getResourceNames().values().forEach(value -> tagsUtil.verifyTags(value, (TestContext) data[0]));
        }
        spotUtil.setUseSpotInstances(Boolean.FALSE);
    }

    /**
     * Overrides the integration test environment creation with setting Telemetry up for E2E resources by default.
     *
     * @param testContext   Spring offers ApplicationContextAware interface to provide configuration of the Integration Test ApplicationContext.
     *                      Should not be null!
     */
    @Override
    protected void createDefaultEnvironment(TestContext testContext) {
        testContext
                .given("telemetry", TelemetryTestDto.class)
                    .withLogging()
                    .withReportClusterLogs()
                .given(EnvironmentTestDto.class)
                    .withTelemetry("telemetry")
                    .withCreateFreeIpa(Boolean.FALSE)
                .when(getEnvironmentTestClient().create())
                .await(EnvironmentStatus.AVAILABLE)
                .when(getEnvironmentTestClient().describe())
                .validate();
    }

    /**
     * Given Cloud Platform is the only supported one for the test. (Invoked in the 'setupTest')
     *
     * If the actual Cloud provider is different as the provided one, it throws {@link AssertionError}.
     *
     * @param cloudPlatform the supported Cloud Platform. (Must not be null)
     */
    protected void assertSupportedCloudPlatform(@Nonnull CloudPlatform cloudPlatform) {
        String cloudProvider = commonCloudProperties().getCloudProvider();
        assertThat(
                String.format("The only supported cloud provider for this test is [%s]. Actual cloud provider is [%s].",
                        cloudPlatform.name(), cloudProvider),
                cloudProvider,
                equalToIgnoringCase(cloudPlatform.name()));
    }

    /**
     * Given Cloud Platform is not supported for the test. (Invoked in the 'setupTest')
     *
     * If the actual Cloud provider is the same as the provided one, it throws {@link AssertionError}.
     *
     * @param cloudPlatform the NOT supported cloud platform. (Must not be null)
     */
    protected void assertNotSupportedCloudPlatform(@Nonnull CloudPlatform cloudPlatform) {
        String cloudProvider = commonCloudProperties().getCloudProvider();
        assertThat(
                String.format("The [%s] cloud provider is NOT supported for this test!", cloudProvider),
                cloudProvider,
                not(equalToIgnoringCase(cloudPlatform.name())));
    }

    protected String getBaseLocationForPreTermination(TestContext testContext) {
        return testContext.getCloudProvider().getBaseLocationForPreTermination();
    }

}
