package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import static com.sequenceiq.it.cloudbreak.assertion.distrox.DistroXExternalDatabaseAssertion.validateTemplateContainsExternalDatabaseHostname;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import com.microsoft.azure.management.resources.ResourceGroup;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseAvailabilityType;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.ResourceGroupTest;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.assertion.distrox.AwsAvailabilityZoneAssertion;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.aws.AwsCloudProvider;
import com.sequenceiq.it.cloudbreak.cloud.v4.azure.AzureCloudProvider;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupsBuilder;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.DistroxUtil;
import com.sequenceiq.it.cloudbreak.util.FreeIpaInstanceUtil;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.it.cloudbreak.util.azure.AzureCloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshEnaDriverCheckActions;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

/**
 * Google Cloud related features are not completed yet. So this E2E test suite is applicable for AWS and Azure right now.
 */
public class DistroXEncryptedVolumeTest extends AbstractE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXEncryptedVolumeTest.class);

    private String resourceGroupForTest;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private SshEnaDriverCheckActions sshEnaDriverCheckActions;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private AwsCloudProvider awsCloudProvider;

    @Inject
    private AzureCloudProvider azureCloudProvider;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private FreeIpaInstanceUtil freeIpaInstanceUtil;

    @Inject
    private SdxUtil sdxUtil;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroxUtil distroxUtil;

    @Inject
    private AzureCloudFunctionality azureCloudFunctionality;

    @Override
    protected void setupTest(TestContext testContext) {
        assertNotSupportedCloudPlatform(CloudPlatform.GCP);
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
    }

    @AfterMethod(onlyForGroups = { "azure_singlerg" }, dependsOnMethods = { "tearDown", "tearDownSpot" })
    public void singleResourceGroupTearDown(Object[] data) {
        LOGGER.info("Delete the '{}' resource group after test has been done!", resourceGroupForTest);
        deleteResourceGroupCreatedForEnvironment(resourceGroupForTest);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a valid credential for the selected provider",
            when = "new environment and freeIpa with encrypted volume should be created",
                and = "SDX then distroX with encrypted volumes also should be created for environment",
            then = "freeIpa, sdx and distroX volumes should be encrypted with the provided key")
    public void testCreateDistroXWithEncryptedVolumes(TestContext testContext) {
        DistroXDatabaseRequest distroXDatabaseRequest = new DistroXDatabaseRequest();
        List<DistroXInstanceGroupTestDto> distroXInstanceGroupTestDtos = new DistroXInstanceGroupsBuilder(testContext)
                .defaultHostGroup()
                .withDiskEncryption()
                .build();
        distroXDatabaseRequest.setAvailabilityType(DistroXDatabaseAvailabilityType.NON_HA);

        testContext
                .given(EnvironmentNetworkTestDto.class)
                .given("telemetry", TelemetryTestDto.class)
                    .withLogging()
                    .withReportClusterLogs()
                .given(EnvironmentTestDto.class)
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
                .then(this::verifyEnvironmentResponseDiskEncryptionKey)
                .given(FreeIpaTestDto.class)
                .then(this::verifyFreeIpaVolumeEncryptionKey)
                .given(SdxTestDto.class)
                    .withCloudStorage()
                .when(sdxTestClient.create())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .then(this::verifySdxVolumeEncryptionKey)
                .given(DistroXTestDto.class)
                    .withInstanceGroupsEntity(distroXInstanceGroupTestDtos)
                    .withExternalDatabase(distroXDatabaseRequest)
                .when(distroXTestClient.create())
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .then(this::verifyDistroxVolumeEncryptionKey)
                .then(this::verifyAwsEnaDriver)
                .then(new AwsAvailabilityZoneAssertion())
                .then(validateTemplateContainsExternalDatabaseHostname())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT, groups = { "azure_singlerg" })
    @UseSpotInstances
    @Description(
            given = "there is a valid Azure credential and a new resource group",
            when = "environment with freeIpa should be created in the resource group with encrypted volume",
                and = "SDX then distroX also with encrypted volumes should be created for environment",
            then = "freeIpa, sdx and distroX volumes should be encrypted with the provided key")
    public void testCreateDistroXWithEncryptedVolumesInSingleRG(TestContext testContext) {
        DistroXDatabaseRequest distroXDatabaseRequest = new DistroXDatabaseRequest();
        List<DistroXInstanceGroupTestDto> distroXInstanceGroupTestDtos = new DistroXInstanceGroupsBuilder(testContext)
                .defaultHostGroup()
                .withDiskEncryption()
                .build();
        distroXDatabaseRequest.setAvailabilityType(DistroXDatabaseAvailabilityType.NON_HA);

        createResourceGroupForEnvironment(testContext);

        testContext
                .given(EnvironmentNetworkTestDto.class)
                .given("telemetry", TelemetryTestDto.class)
                    .withLogging()
                    .withReportClusterLogs()
                .given(EnvironmentTestDto.class)
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
                .then(this::verifyEnvironmentResponseDiskEncryptionKey)
                .given(FreeIpaTestDto.class)
                .then((context, testDto, testClient) -> verifyFreeIpaVolumeEncryptionKey(context, testDto, testClient, resourceGroupForTest))
                .given(SdxTestDto.class)
                    .withCloudStorage()
                .when(sdxTestClient.create())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .then((context, testDto, testClient) -> verifySdxVolumeEncryptionKey(context, testDto, testClient, resourceGroupForTest))
                .given(DistroXTestDto.class)
                    .withInstanceGroupsEntity(distroXInstanceGroupTestDtos)
                    .withExternalDatabase(distroXDatabaseRequest)
                .when(distroXTestClient.create())
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .then((context, testDto, testClient) -> verifyDistroxVolumeEncryptionKey(context, testDto, testClient, resourceGroupForTest))
                .then(this::verifyAwsEnaDriver)
                .then(new AwsAvailabilityZoneAssertion())
                .then(validateTemplateContainsExternalDatabaseHostname())
                .validate();
    }

    private ResourceGroup createResourceGroupForEnvironment(TestContext testContext) {
        resourceGroupForTest = resourcePropertyProvider().getName();
        String username = StringUtils.substringBefore(testContext.getActingUserCrn().getResource(), "@").toLowerCase();
        Map<String, String> tags = new HashMap<>() {{
            put("owner", username);
            put("creation-timestamp", String.valueOf(new Date().getTime()));
        }};
        return azureCloudFunctionality.createResourceGroup(resourceGroupForTest, tags);
    }

    private void deleteResourceGroupCreatedForEnvironment(String resourceGroupName) {
        azureCloudFunctionality.deleteResourceGroup(resourceGroupName);
    }

    private DistroXTestDto verifyAwsEnaDriver(TestContext testContext, DistroXTestDto testDto, CloudbreakClient cloudbreakClient) {
        if (CloudPlatform.AWS.equals(testDto.getCloudPlatform())) {
            sshEnaDriverCheckActions.checkEnaDriverOnAws(testDto.getResponse(), cloudbreakClient);
        } else {
            LOGGER.warn(format("ENA driver is only available at AWS. So validation on '%s' provider is not possible!", testDto.getCloudPlatform()));
        }
        return testDto;
    }

    private EnvironmentTestDto verifyEnvironmentResponseDiskEncryptionKey(TestContext testContext, EnvironmentTestDto testDto,
            EnvironmentClient environmentClient) {
        DetailedEnvironmentResponse environment = environmentClient.getDefaultClient().environmentV1Endpoint().getByName(testDto.getName());
        if (CloudPlatform.AWS.name().equals(environment.getCloudPlatform())) {
            String encryptionKeyArn = environment.getAws().getAwsDiskEncryptionParameters().getEncryptionKeyArn();
            verifyDiskEncryptionKey(testDto.getCloudPlatform(), encryptionKeyArn, testDto.getRequest().getName());
        } else if (CloudPlatform.AZURE.name().equals(environment.getCloudPlatform())) {
            String diskEncryptionSetId = environment.getAzure().getResourceEncryptionParameters().getDiskEncryptionSetId();
            verifyDiskEncryptionKey(testDto.getCloudPlatform(), diskEncryptionSetId, testDto.getRequest().getName());
        } else {
            LOGGER.warn(format("Disk encryption feature is not available at '%s' provider currently!", environment.getCloudPlatform()));
        }
        return testDto;
    }

    private void verifyDiskEncryptionKey(CloudPlatform cloudPlatform, String encryptionKey, String environmentName) {
        if (CloudPlatform.AWS.equals(cloudPlatform)) {
            if (StringUtils.isEmpty(encryptionKey)) {
                LOGGER.error(format("KMS key is not available for '%s' environment!", environmentName));
                throw new TestFailException(format("KMS key is not available for '%s' environment!", environmentName));
            } else {
                LOGGER.info(format("Environment '%s' create has been done with '%s' KMS key.", environmentName, encryptionKey));
                Log.then(LOGGER, format(" Environment '%s' create has been done with '%s' KMS key. ", environmentName, encryptionKey));
            }
        } else if (CloudPlatform.AZURE.equals(cloudPlatform)) {
            if (StringUtils.isEmpty(encryptionKey)) {
                LOGGER.error(format("DES key is not available for '%s' environment!", environmentName));
                throw new TestFailException(format("DES key is not available for '%s' environment!", environmentName));
            } else {
                LOGGER.info(format("Environment '%s' create has been done with '%s' DES key.", environmentName, encryptionKey));
                Log.then(LOGGER, format(" Environment '%s' create has been done with '%s' DES key. ", environmentName, encryptionKey));
            }
        }
    }

    private FreeIpaTestDto verifyFreeIpaVolumeEncryptionKey(TestContext testContext, FreeIpaTestDto testDto, FreeIpaClient freeIpaClient) {
        return verifyFreeIpaVolumeEncryptionKey(testContext, testDto, freeIpaClient, null);
    }

    private FreeIpaTestDto verifyFreeIpaVolumeEncryptionKey(TestContext testContext, FreeIpaTestDto testDto, FreeIpaClient freeIpaClient,
            String resourceGroupName) {
        CloudFunctionality cloudFunctionality = testContext.getCloudProvider().getCloudFunctionality();
        List<String> instanceIds = freeIpaInstanceUtil.getInstanceIds(testDto, freeIpaClient, MASTER.getName());
        verifyVolumeEncryptionKey(testDto.getCloudPlatform(), testDto.getName(), instanceIds, cloudFunctionality,
                testContext.given(EnvironmentTestDto.class).getRequest().getName(), resourceGroupName);
        return testDto;
    }

    private SdxTestDto verifySdxVolumeEncryptionKey(TestContext testContext, SdxTestDto testDto, SdxClient sdxClient) {
        return verifySdxVolumeEncryptionKey(testContext, testDto, sdxClient, null);
    }

    private SdxTestDto verifySdxVolumeEncryptionKey(TestContext testContext, SdxTestDto testDto, SdxClient sdxClient, String resourceGroupName) {
        CloudFunctionality cloudFunctionality = testContext.getCloudProvider().getCloudFunctionality();
        List<String> instanceIds = sdxUtil.getInstanceIds(testDto, sdxClient, MASTER.getName());
        verifyVolumeEncryptionKey(testDto.getCloudPlatform(), testDto.getName(), instanceIds, cloudFunctionality,
                testContext.given(EnvironmentTestDto.class).getRequest().getName(), resourceGroupName);
        return testDto;
    }

    private DistroXTestDto verifyDistroxVolumeEncryptionKey(TestContext testContext, DistroXTestDto testDto, CloudbreakClient cloudbreakClient) {
        return verifyDistroxVolumeEncryptionKey(testContext, testDto, cloudbreakClient, null);
    }

    private DistroXTestDto verifyDistroxVolumeEncryptionKey(TestContext testContext, DistroXTestDto testDto, CloudbreakClient cloudbreakClient,
            String resourceGroupName) {
        CloudFunctionality cloudFunctionality = testContext.getCloudProvider().getCloudFunctionality();
        List<String> instanceIds = distroxUtil.getInstanceIds(testDto, cloudbreakClient, MASTER.getName());
        verifyVolumeEncryptionKey(testDto.getCloudPlatform(), testDto.getName(), instanceIds, cloudFunctionality,
                testContext.given(EnvironmentTestDto.class).getRequest().getName(), resourceGroupName);
        return testDto;
    }

    private void verifyVolumeEncryptionKey(CloudPlatform cloudPlatform, String resourceName, List<String> instanceIds,
            CloudFunctionality cloudFunctionality, String environmentName, String resourceGroupName) {
        if (CloudPlatform.AWS.equals(cloudPlatform)) {
            String kmsKeyArn = awsCloudProvider.getEncryptionKeyArn(true);
            List<String> volumeKmsKeyIds = new ArrayList<>(cloudFunctionality.listVolumeEncryptionKeyIds(resourceName, null, instanceIds));
            if (volumeKmsKeyIds.stream().noneMatch(keyId -> keyId.equalsIgnoreCase(kmsKeyArn))) {
                LOGGER.error(format("Volume has not been encrypted with '%s' KMS key!", kmsKeyArn));
                throw new TestFailException(format("Volume has not been encrypted with '%s' KMS key!", kmsKeyArn));
            } else {
                LOGGER.info(format("Volume has been encrypted with '%s' KMS key.", kmsKeyArn));
                Log.then(LOGGER, format(" Volume has been encrypted with '%s' KMS key. ", kmsKeyArn));
            }
        } else if (CloudPlatform.AZURE.equals(cloudPlatform)) {
            String desKeyUrl = azureCloudProvider.getEncryptionKeyUrl();
            List<String> volumesDesId = new ArrayList<>(cloudFunctionality.listVolumeEncryptionKeyIds(resourceName, resourceGroupName, instanceIds));
            volumesDesId.forEach(desId -> {
                if (desId.contains("diskEncryptionSets/" + environmentName)) {
                    LOGGER.info(format("Volume has been encrypted with '%s' DES key.", desId));
                    Log.then(LOGGER, format(" Volume has been encrypted with '%s' DES key. ", desId));
                } else {
                    LOGGER.error(format("Volume has not been encrypted with '%s' key!", desKeyUrl));
                    throw new TestFailException(format("Volume has not been encrypted with '%s' key!", desKeyUrl));
                }
            });
        } else {
            LOGGER.warn(format("Disk encryption feature is not available at '%s' currently!", cloudPlatform));
        }
    }
}
