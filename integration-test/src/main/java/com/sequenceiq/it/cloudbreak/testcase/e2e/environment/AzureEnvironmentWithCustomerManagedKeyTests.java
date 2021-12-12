package com.sequenceiq.it.cloudbreak.testcase.e2e.environment;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import com.microsoft.azure.management.resources.ResourceGroup;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.ResourceGroupTest;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.azure.AzureCloudProvider;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.FreeIpaInstanceUtil;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;

public class AzureEnvironmentWithCustomerManagedKeyTests extends AbstractE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsEnvironmentWithCustomerManagedKeyTests.class);

    private String resourceGroupForTest;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private AzureCloudProvider azureCloudProvider;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private FreeIpaInstanceUtil freeIpaInstanceUtil;

    @Override
    protected void setupTest(TestContext testContext) {
        checkCloudPlatform(CloudPlatform.AZURE);
        createDefaultUser(testContext);
    }

    @AfterMethod(onlyForGroups = { "withrg" })
    public void tearDown() {
        LOGGER.info("Delete the '{}' resource group after test has been done!", resourceGroupForTest);
        deleteResourceGroupCreatedForEnvironment(resourceGroupForTest);
    }

    @Test(dataProvider = TEST_CONTEXT, groups = { "withrg" }, description = "Creating a resource group for this test case. " +
            "Disk encryption set(DES) is created in this newly created RG, as cloud-daily RG has locks which prevents cleanup of DES.")
    @UseSpotInstances
    @Description(
            given = "there is a running cloudbreak",
            when = "create an Environment with disk encryption where key and environment are in same Resource groups",
            then = "should use encryption parameters for resource encryption.")
    public void testWithEnvironmentResourceGroup(TestContext testContext) {
        String environmentName = resourcePropertyProvider().getName();

        resourceGroupForTest = resourcePropertyProvider().getName();
        createResourceGroupForEnvironment(resourceGroupForTest);

        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(EnvironmentNetworkTestDto.class)
                .given("telemetry", TelemetryTestDto.class)
                    .withLogging()
                    .withReportClusterLogs()
                .given(EnvironmentTestDto.class)
                    .withName(environmentName)
                    .withNetwork()
                    .withResourceGroup(ResourceGroupTest.AZURE_RESOURCE_GROUP_USAGE_SINGLE, resourceGroupForTest)
                    .withResourceEncryption()
                    .withTelemetry("telemetry")
                    .withTunnel(Tunnel.CLUSTER_PROXY)
                    .withCreateFreeIpa(Boolean.FALSE)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIpaTestDto.class)
                    .withEnvironment()
                    .withTelemetry("telemetry")
                    .withCatalog(commonCloudProperties().getImageValidation().getFreeIpaImageCatalog(),
                        commonCloudProperties().getImageValidation().getFreeIpaImageUuid())
                .when(freeIpaTestClient.create())
                .await(Status.AVAILABLE)
                .awaitForHealthyInstances()
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.getLastSyncOperationStatus())
                .await(OperationState.COMPLETED)
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.describe())
                .then(this::verifyEnvironmentResponseDesParameters)
                .given(FreeIpaTestDto.class)
                .then((context, testDto, testClient) -> verifyFreeIpaVolumeDesKey(context, testDto, testClient, environmentName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT, groups = { "norg" }, description = "Environment's Resource Group is not specified, in this case all the" +
            " resources create their own resource groups.")
    @UseSpotInstances
    @Description(
            given = "there is a running cloudbreak",
            when = "create an Environment with disk encryption where key and environment are in different Resource groups",
            then = "should use encryption parameters for resource encryption.")
    public void testWithEncryptionKeyResourceGroup(TestContext testContext) {
        String environmentName = resourcePropertyProvider().getName();

        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(EnvironmentNetworkTestDto.class)
                .given("telemetry", TelemetryTestDto.class)
                    .withLogging()
                    .withReportClusterLogs()
                .given(EnvironmentTestDto.class)
                    .withName(environmentName)
                    .withNetwork()
                    .withResourceEncryption()
                    .withTelemetry("telemetry")
                    .withTunnel(Tunnel.CLUSTER_PROXY)
                    .withCreateFreeIpa(Boolean.FALSE)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIpaTestDto.class)
                    .withEnvironment()
                    .withTelemetry("telemetry")
                    .withCatalog(commonCloudProperties().getImageValidation().getFreeIpaImageCatalog(),
                        commonCloudProperties().getImageValidation().getFreeIpaImageUuid())
                .when(freeIpaTestClient.create())
                .await(Status.AVAILABLE)
                .awaitForHealthyInstances()
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.getLastSyncOperationStatus())
                .await(OperationState.COMPLETED)
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.describe())
                .then(this::verifyEnvironmentResponseDesParameters)
                .given(FreeIpaTestDto.class)
                .then((context, testDto, testClient) -> verifyFreeIpaVolumeDesKey(context, testDto, testClient, environmentName))
                .validate();
    }

    private ResourceGroup createResourceGroupForEnvironment(String resourceGroupName) {
        CloudFunctionality cloudFunctionality = azureCloudProvider.getCloudFunctionality();
        return cloudFunctionality.createResourceGroup(resourceGroupName);
    }

    private void deleteResourceGroupCreatedForEnvironment(String resourceGroupName) {
        CloudFunctionality cloudFunctionality = azureCloudProvider.getCloudFunctionality();
        cloudFunctionality.deleteResourceGroup(resourceGroupName);
    }

    private EnvironmentTestDto verifyEnvironmentResponseDesParameters(TestContext testContext, EnvironmentTestDto testDto,
            EnvironmentClient environmentClient) {
        DetailedEnvironmentResponse environment = environmentClient.getDefaultClient().environmentV1Endpoint().getByName(testDto.getName());

        if (CloudPlatform.AZURE.name().equals(environment.getCloudPlatform())) {
            String encryptionKey = environment.getAzure().getResourceEncryptionParameters().getEncryptionKeyUrl();
            String environmentName = testDto.getRequest().getName();

            if (StringUtils.isEmpty(encryptionKey)) {
                LOGGER.error(String.format("DES key is not available for '%s' environment!", environmentName));
                throw new TestFailException(format("DES key is not available for '%s' environment!", environmentName));
            } else {
                LOGGER.info(String.format(" Environment '%s' create has been done with '%s' DES key. ", environmentName, encryptionKey));
                Log.then(LOGGER, format(" Environment '%s' create has been done with '%s' DES key. ", environmentName, encryptionKey));
            }
        }
        return testDto;
    }

    private FreeIpaTestDto verifyFreeIpaVolumeDesKey(TestContext testContext, FreeIpaTestDto testDto, FreeIpaClient freeIpaClient, String environmentName) {
        CloudFunctionality cloudFunctionality = testContext.getCloudProvider().getCloudFunctionality();
        List<String> instanceIds = freeIpaInstanceUtil.getInstanceIds(testDto, freeIpaClient, MASTER.getName());
        String desKeyUrl = azureCloudProvider.getEncryptionKeyUrl();

        List<String> volumesDesId = new ArrayList<>(cloudFunctionality.listVolumeEncryptionKeyIds(testDto.getRequest().getName(), instanceIds));
        volumesDesId.forEach(desId -> {
            if (desId.contains("diskEncryptionSets/" + environmentName)) {
                LOGGER.info(format("FreeIpa volume has been encrypted with '%s' DES key!", desId));
                Log.then(LOGGER, format(" FreeIpa volume has not been encrypted with '%s' DES key! ", desId));
            } else {
                LOGGER.error(format("FreeIpa volume has not been encrypted with '%s' DES key!", desKeyUrl));
                throw new TestFailException(format("FreeIpa volume has not been encrypted with '%s' DES key!", desKeyUrl));
            }
        });
        return testDto;
    }
}