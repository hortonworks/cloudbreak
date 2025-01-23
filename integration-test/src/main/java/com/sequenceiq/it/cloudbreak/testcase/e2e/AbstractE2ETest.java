package com.sequenceiq.it.cloudbreak.testcase.e2e;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.not;

import java.util.Map;

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Listeners;

import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.assertion.encryption.SecretEncryptionAssertions;
import com.sequenceiq.it.cloudbreak.assertion.safelogic.SafeLogicAssertions;
import com.sequenceiq.it.cloudbreak.config.azure.ResourceGroupProperties;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.cloudbreak.util.azure.AzureCloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.spot.SpotRetryOnceTestListener;
import com.sequenceiq.it.cloudbreak.util.spot.SpotRetryUtil;
import com.sequenceiq.it.cloudbreak.util.spot.SpotUtil;
import com.sequenceiq.it.util.TagsUtil;
import com.sequenceiq.it.util.imagevalidation.ImageValidatorE2ETest;
import com.sequenceiq.it.util.imagevalidation.ImageValidatorE2ETestUtil;

@Listeners(SpotRetryOnceTestListener.class)
public abstract class AbstractE2ETest extends AbstractIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractE2ETest.class);

    @Inject
    private SpotUtil spotUtil;

    @Inject
    private SpotRetryUtil spotRetryUtil;

    @Inject
    private TagsUtil tagsUtil;

    @Inject
    private AzureCloudFunctionality azureCloudFunctionality;

    @Inject
    private ResourceGroupProperties resourceGroupProperties;

    @Inject
    private SafeLogicAssertions safeLogicAssertions;

    @Inject
    private SecretEncryptionAssertions secretEncryptionAssertions;

    @Inject
    private ImageValidatorE2ETestUtil imageValidatorE2ETestUtil;

    @Override
    protected void setupTest(ITestResult testResult) {
        boolean shouldUseSpotInstances = spotUtil.shouldUseSpotInstancesForTest(testResult.getMethod().getConstructorOrMethod().getMethod());
        boolean retried = spotRetryUtil.isRetried(testResult.getMethod());
        spotUtil.setUseSpotInstances(shouldUseSpotInstances && !retried);
        createResourceGroup((TestContext) testResult.getParameters()[0]);
    }

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
        initializeAzureMarketplaceTermsPolicy(testContext);
        createDefaultEnvironment(testContext);
    }

    @AfterMethod
    public void tearDownAbstract(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        if (this instanceof ImageValidatorE2ETest imageValidatorE2ETest) {
            imageValidatorE2ETestUtil.validateImageIdAndWriteToFile(testContext, imageValidatorE2ETest);
        }
        if (MapUtils.isEmpty(testContext.getExceptionMap())) {
            LOGGER.info("Validating default tags on the created and tagged test resources...");
            testContext.getResourceNames().values().forEach(value -> tagsUtil.verifyTags(value, testContext));
            if (testContext.getSafeLogicValidation()) {
                LOGGER.info("Validating SafeLogic installation of created resources...");
                safeLogicAssertions.validate(testContext);
            }
            LOGGER.info("Validating secret encryption on the created resources...");
            //TODO reenable this once https://jira.cloudera.com/browse/CB-28133 is fixed
//            secretEncryptionAssertions.validateAllExisting(testContext);
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
                    .withResourceEncryption(testContext.isResourceEncryptionEnabled())
                    .withCreateFreeIpa(Boolean.FALSE)
                .when(getEnvironmentTestClient().create())
                .await(EnvironmentStatus.AVAILABLE)
                .when(getEnvironmentTestClient().describe())
                .validate();
    }

    @Override
    protected void createEnvironmentWithFreeIpa(TestContext testContext) {
        createResourceGroup(testContext);
        super.createEnvironmentWithFreeIpa(testContext);
    }

    @Override
    protected void createDefaultDatalake(TestContext testContext) {
        createResourceGroup(testContext);
        super.createDefaultDatalake(testContext);
    }

    @Override
    protected void createDatalakeWithVersion(TestContext testContext, String runtimeVersion) {
        createResourceGroup(testContext);
        super.createDatalakeWithVersion(testContext, runtimeVersion);
    }

    @Override
    protected void createDefaultDatahub(TestContext testContext) {
        createResourceGroup(testContext);
        super.createDefaultDatahub(testContext);
    }

    @Override
    protected void createStorageOptimizedDatahub(TestContext testContext) {
        createResourceGroup(testContext);
        super.createStorageOptimizedDatahub(testContext);
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

    private void createResourceGroup(TestContext testContext) {
        String cloudProvider = commonCloudProperties().getCloudProvider();

        if (CloudPlatform.AZURE.name().equalsIgnoreCase(cloudProvider)) {
            Map<String, String> tags = Map.of("owner", testContext.getActingUserOwnerTag(),
                    "creation-timestamp", testContext.getCreationTimestampTag());
            ResourceGroup temporaryResourceGroup = azureCloudFunctionality.createResourceGroup(resourceGroupProperties.getResourceGroupName(), tags);
            LOGGER.info("The temporary single resource group '{}' for E2E tests has been provisioned with status '{}' before test has been started!",
                    temporaryResourceGroup.name(), temporaryResourceGroup.provisioningState());
        } else {
            LOGGER.info("Cloud provider is '{}' for E2E tests. So do not need to check then create E2E test resource group for Azure.", cloudProvider);
        }
    }
}
