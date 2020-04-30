package com.sequenceiq.it.cloudbreak.testcase.e2e;

import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.junit.Assert.assertThat;

import javax.annotation.Nonnull;

import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Listeners;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.cloudbreak.util.spot.SpotRetryOnceTestListener;
import com.sequenceiq.it.cloudbreak.util.spot.SpotRetryUtil;
import com.sequenceiq.it.cloudbreak.util.spot.SpotUtil;

@Listeners(SpotRetryOnceTestListener.class)
public abstract class AbstractE2ETest extends AbstractIntegrationTest {

    @Override
    protected void setupTest(ITestResult testResult) {
        boolean shouldUseSpotInstances = SpotUtil.shouldUseSpotInstances(testResult.getMethod().getConstructorOrMethod().getMethod());
        boolean retried = SpotRetryUtil.isRetried(testResult.getMethod());
        SpotUtil.useSpotInstances(shouldUseSpotInstances && !retried);
    }

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultEnvironment(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @AfterMethod
    public void tearDownSpot() {
        SpotUtil.useSpotInstances(Boolean.FALSE);
    }

    /**
     * Checks the cloud platform when a specific cloud provider is the only one which is needed.
     * It throws {@link AssertionError} if the cloud provider doesn't match.
     *
     * @param cloudPlatform the required/expected cloud platform. Must not be null.
     */
    protected void checkCloudPlatform(@Nonnull CloudPlatform cloudPlatform) {
        String cloudProvider = commonCloudProperties().getCloudProvider();
        assertThat(
                String.format("The only supported cloud provider for this test is [%s]. Actual cloud provider is [%s].",
                        cloudPlatform.name(), cloudProvider),
                cloudProvider, equalToIgnoringCase(cloudPlatform.name()));
    }

}
