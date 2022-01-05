package com.sequenceiq.it.cloudbreak.testcase.e2e;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.not;

import javax.annotation.Nonnull;
import javax.inject.Inject;

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

    @Inject
    private SpotUtil spotUtil;

    @Inject
    private SpotRetryUtil spotRetryUtil;

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
    public void tearDownSpot() {
        spotUtil.setUseSpotInstances(Boolean.FALSE);
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
}
