package com.sequenceiq.it.cloudbreak.testcase.e2e.environment;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;

public class EnvironmentWithCustomerManagedKeyTests extends AbstractE2ETest {

    private static final String ENCRYPTION_KEY_URL = "https://cloud-jenkins-secrets.vault.azure.net/keys/key-azure-cmk-e2e/c1a1df3097f441b4a10cfcb7c8a3bcd9";

    private static final String ENCRYPTION_KEY_URL_RESOURCE_GROUP = "cloud-daily";

    private static final String RESOURCE_GROUP_FOR_TEST = "azure-test-des-rg";

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private Azure azure;

    @Inject
    private CommonCloudProperties commonCloudProperties;

    @Override
    protected void setupTest(TestContext testContext) {
        checkCloudPlatform(CloudPlatform.AZURE);
        createDefaultUser(testContext);
    }

    @AfterMethod
    public void tearDownSpot() {
        deleteResourceGroupCreatedForEnvironment(RESOURCE_GROUP_FOR_TEST);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running cloudbreak",
            when = "create an Environment with encryption parameters where key and environment are in different Resource groups",
            then = "should use encryption parameters for resource encryption.")
    public void testEnvironmentWithRGSpecifiedForEnv(TestContext testContext) {
        // testEnvironmentWithResourceGroupSpecifiedForEnvironment
        // Creating a resource group for this test case. Disk encryption set(DES) is created in this newly created RG, as cloud-daily RG
        // has locks which prevents cleanup of DES.
        createResourceGroupForEnvironment(RESOURCE_GROUP_FOR_TEST);

        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                .given(EnvironmentTestDto.class)
                .withName("azure-test-des-1")
                .withNetwork()
                .withResourceGroup("SINGLE", RESOURCE_GROUP_FOR_TEST)
                .withAzureResourceEncryptionParameters(ENCRYPTION_KEY_URL, ENCRYPTION_KEY_URL_RESOURCE_GROUP)
                .withTelemetry("telemetry")
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .then((tc, testDto, cc) -> environmentTestClient.describe().action(tc, testDto, cc))
                .then(verifyEncryptionParameters())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running cloudbreak",
            when = "create an Environment with encryption parameters where key and environment are in different Resource groups",
            then = "should use encryption parameters for resource encryption.")
    public void testEnvironmentWithRGSpecifiedForKey(TestContext testContext) {
        // testEnvironmentWithResourceGroupSpecifiedForEncryptionKey
        // Environment's Resource Group is not specified, in this case all the resources create their own resource groups.
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                .given(EnvironmentTestDto.class)
                .withName("azure-test-des-2")
                .withNetwork()
                .withResourceGroup("USE_MULTIPLE", null)
                .withAzureResourceEncryptionParameters(ENCRYPTION_KEY_URL, ENCRYPTION_KEY_URL_RESOURCE_GROUP)
                .withTelemetry("telemetry")
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .then((tc, testDto, cc) -> environmentTestClient.describe().action(tc, testDto, cc))
                .then(verifyEncryptionParameters())
                .validate();
    }

    private ResourceGroup createResourceGroupForEnvironment(String resourceGroupName) {
        return azure.resourceGroups().define(resourceGroupName)
                .withRegion("West US 2")
                .withTags(commonCloudProperties.getTags())
                .create();
    }

    private boolean resourceGroupExists(String name) {
        try {
            return azure.resourceGroups().contain(name);
        } catch (CloudException e) {
            if (e.getMessage().contains("Status code 403")) {
                return false;
            }
            throw e;
        }
    }

    private void deleteResourceGroupCreatedForEnvironment(String resourceGroupName) {
        if (resourceGroupExists(resourceGroupName)) {
            azure.resourceGroups().deleteByName(resourceGroupName);
        }
    }

    private static Assertion<EnvironmentTestDto, EnvironmentClient> verifyEncryptionParameters() {
        return (testContext, testDto, environmentClient) -> {
            DetailedEnvironmentResponse environment = environmentClient.getDefaultClient().environmentV1Endpoint().getByName(testDto.getName());
            if (CloudPlatform.AZURE.name().equals(environment.getCloudPlatform())) {
                if (StringUtils.isEmpty(environment.getAzure().getResourceEncryptionParameters().getDiskEncryptionSetId())) {
                    throw new IllegalArgumentException("Failed to create disk encryption set.");
                }
            }
            return testDto;
        };
    }
}