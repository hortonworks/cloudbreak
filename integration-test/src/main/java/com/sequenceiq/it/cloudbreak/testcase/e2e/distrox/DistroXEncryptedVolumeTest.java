package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.PRE_TERMINATION;
import static com.sequenceiq.it.cloudbreak.assertion.distrox.DistroXExternalDatabaseAssertion.validateTemplateContainsExternalDatabaseHostname;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseAvailabilityType;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.it.cloudbreak.ResourceGroupTest;
import com.sequenceiq.it.cloudbreak.assertion.distrox.AwsAvailabilityZoneAssertion;
import com.sequenceiq.it.cloudbreak.assertion.salt.SaltHighStateDurationAssertions;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.aws.AwsCloudProvider;
import com.sequenceiq.it.cloudbreak.cloud.v4.azure.AzureCloudProvider;
import com.sequenceiq.it.cloudbreak.cloud.v4.gcp.GcpCloudProvider;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupsBuilder;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;
import com.sequenceiq.it.cloudbreak.dto.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.DistroxUtil;
import com.sequenceiq.it.cloudbreak.util.FreeIpaInstanceUtil;
import com.sequenceiq.it.cloudbreak.util.RecipeUtil;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.it.cloudbreak.util.azure.AzureCloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

/**
 * Google Cloud related features are not completed yet. So this E2E test suite is applicable for AWS and Azure right now.
 */
public class DistroXEncryptedVolumeTest extends AbstractE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXEncryptedVolumeTest.class);

    private String resourceGroupForTest;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private AwsCloudProvider awsCloudProvider;

    @Inject
    private AzureCloudProvider azureCloudProvider;

    @Inject
    private GcpCloudProvider gcpCloudProvider;

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

    @Inject
    private RecipeTestClient recipeTestClient;

    @Inject
    private RecipeUtil recipeUtil;

    @Inject
    private SaltHighStateDurationAssertions saltHighStateDurationAssertions;

    @Inject
    private EncryptedTestUtil encryptedTestUtil;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
    }

    @AfterMethod(onlyForGroups = "azure_singlerg", dependsOnMethods = { "tearDown", "tearDownAbstract"})
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
        String preTerminationRecipeName = resourcePropertyProvider().getName();
        DistroXDatabaseRequest distroXDatabaseRequest = new DistroXDatabaseRequest();

        prepareRecipe(testContext, preTerminationRecipeName);

        List<DistroXInstanceGroupTestDto> distroXInstanceGroupTestDtos =
                getDistroXInstanceGroupTestDtoWithEncryptionAndRecipe(testContext, preTerminationRecipeName);
        distroXDatabaseRequest.setAvailabilityType(DistroXDatabaseAvailabilityType.NON_HA);

        encryptedTestUtil.createEnvironment(testContext);
        encryptedTestUtil.createFreeipa(testContext, commonCloudProperties());
        encryptedTestUtil.doFreeipUserSync(testContext);
        encryptedTestUtil.assertEnvironmentAndFreeipa(testContext, resourceGroupForTest);
        encryptedTestUtil.createDatalake(testContext);
        encryptedTestUtil.assertDatalake(testContext, resourceGroupForTest);
        encryptedTestUtil.createDatahub(testContext, distroXDatabaseRequest, distroXInstanceGroupTestDtos);
        encryptedTestUtil.assertDatahub(testContext, resourceGroupForTest);
        encryptedTestUtil.deleteDatahub(testContext, preTerminationRecipeName, testContext.getCloudProvider().getBaseLocationForPreTermination());
    }

    @Test(dataProvider = TEST_CONTEXT, groups = "azure_singlerg")
    @UseSpotInstances
    @Description(
            given = "there is a valid Azure credential and a new resource group",
            when = "environment with freeIpa should be created in the resource group with encrypted volume",
            and = "SDX then distroX also with encrypted volumes should be created for environment",
            then = "freeIpa, sdx and distroX volumes should be encrypted with the provided key")
    public void testCreateDistroXWithEncryptedVolumesInSingleRG(TestContext testContext) {
        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NON_HA);
        DistroXDatabaseRequest distroXDatabaseRequest = new DistroXDatabaseRequest();
        List<DistroXInstanceGroupTestDto> distroXInstanceGroupTestDtos = new DistroXInstanceGroupsBuilder(testContext)
                .defaultHostGroup()
                .withDiskEncryption()
                .build();
        distroXDatabaseRequest.setAvailabilityType(DistroXDatabaseAvailabilityType.NON_HA);

        ResourceGroup createdResourceGroup = createResourceGroupForEnvironment(testContext);
        LOGGER.info("The single resource group '{}' has been provisioned with status '{}' before test has been started!", createdResourceGroup.name(),
                createdResourceGroup.provisioningState());

        testContext
                //environment with telemetry
                .given(EnvironmentNetworkTestDto.class)
                .withServiceEndpoints(ServiceEndpointCreation.DISABLED)
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                .given(EnvironmentTestDto.class)
                .withNetwork()
                .withResourceGroup(ResourceGroupTest.AZURE_RESOURCE_GROUP_USAGE_SINGLE, resourceGroupForTest)
                .withResourceEncryption(Boolean.TRUE)
                .withTelemetry("telemetry")
                .withTunnel(testContext.getTunnel())
                .withCreateFreeIpa(Boolean.FALSE)
                .when(environmentTestClient.create())
                //assert cloudprovider
                .then((context, dto, client) -> {
                    context.getCloudProviderAssertion().assertServiceEndpoint(dto);
                    return dto;
                })
                .await(EnvironmentStatus.AVAILABLE)
                //freeipa from catalog and sync
                .given(FreeIpaTestDto.class)
                .withEnvironment()
                .withTelemetry("telemetry")
                .when(freeIpaTestClient.create())
                .await(Status.AVAILABLE)
                .awaitForHealthyInstances()
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.getLastSyncOperationStatus())
                .await(OperationState.COMPLETED)
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.describe())
                //assert disc encrypt
                .then(this::verifyEnvironmentResponseDiskEncryptionKey)
                .given(FreeIpaTestDto.class)
                .then(this::verifyFreeIpaVolumeEncryptionKey)
                //create sdx
                .given(SdxTestDto.class)
                .withCloudStorage()
                .withExternalDatabase(sdxDatabaseRequest)
                .when(sdxTestClient.create())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                //assert sdx
                .then(this::verifySdxVolumeEncryptionKey)
                //create dh
                .given(DistroXTestDto.class)
                .withInstanceGroupsEntity(distroXInstanceGroupTestDtos)
                .withExternalDatabase(distroXDatabaseRequest)
                .when(distroXTestClient.create())
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                //assert encryption
                .then(this::verifyDistroxVolumeEncryptionKey)
                .then(this::verifyAwsEnaDriver)
                .then(new AwsAvailabilityZoneAssertion())
                .then(validateTemplateContainsExternalDatabaseHostname())
                .validate();
    }

    private void prepareRecipe(TestContext testContext, String preTerminationRecipeName) {
        testContext.given(RecipeTestDto.class)
                    .withName(preTerminationRecipeName)
                    .withContent(recipeUtil.generatePreTerminationRecipeContentForE2E(applicationContext, preTerminationRecipeName))
                    .withRecipeType(PRE_TERMINATION)
                .when(recipeTestClient.createV4());
    }

    private static List<DistroXInstanceGroupTestDto> getDistroXInstanceGroupTestDtoWithEncryptionAndRecipe(TestContext testContext,
            String preTerminationRecipeName) {
        return new DistroXInstanceGroupsBuilder(testContext)
                .defaultHostGroup()
                .withDiskEncryption()
                .withRecipes(Set.of(preTerminationRecipeName))
                .build();
    }

    private ResourceGroup createResourceGroupForEnvironment(TestContext testContext) {
        resourceGroupForTest = resourcePropertyProvider().getName();
        Map<String, String> tags = Map.of("owner", testContext.getActingUserOwnerTag(),
                "creation-timestamp", testContext.getCreationTimestampTag());
        return azureCloudFunctionality.createResourceGroup(resourceGroupForTest, tags);
    }

    private void deleteResourceGroupCreatedForEnvironment(String resourceGroupName) {
        azureCloudFunctionality.deleteResourceGroup(resourceGroupName);
    }

    private DistroXTestDto verifyAwsEnaDriver(TestContext testContext, DistroXTestDto testDto, CloudbreakClient cloudbreakClient) {
        testContext.getCloudProvider().getCloudFunctionality().verifyEnaDriver(testDto.getResponse(), cloudbreakClient);
        return testDto;
    }

    private EnvironmentTestDto verifyEnvironmentResponseDiskEncryptionKey(TestContext testContext, EnvironmentTestDto testDto,
            EnvironmentClient environmentClient) {
        DetailedEnvironmentResponse environment = environmentClient.getDefaultClient().environmentV1Endpoint().getByName(testDto.getName());
        testContext.getCloudProvider().verifyDiskEncryptionKey(environment, testDto.getRequest().getName());
        return testDto;
    }

    private FreeIpaTestDto verifyFreeIpaVolumeEncryptionKey(TestContext testContext, FreeIpaTestDto testDto, FreeIpaClient freeIpaClient) {
        List<String> instanceIds = freeIpaInstanceUtil.getInstanceIds(testDto, freeIpaClient, MASTER.getName());
        List<String> volumeKmsKeyIds = new ArrayList<>(testContext.getCloudProvider().getCloudFunctionality()
                .listVolumeEncryptionKeyIds(testDto.getName(), resourceGroupForTest, instanceIds));
        testContext.getCloudProvider().verifyVolumeEncryptionKey(volumeKmsKeyIds, testContext.given(EnvironmentTestDto.class).getRequest().getName());
        return testDto;
    }

    private SdxTestDto verifySdxVolumeEncryptionKey(TestContext testContext, SdxTestDto testDto, SdxClient sdxClient) {
        List<String> instanceIds = sdxUtil.getInstanceIds(testDto, sdxClient, MASTER.getName());
        List<String> volumeKmsKeyIds = new ArrayList<>(testContext.getCloudProvider().getCloudFunctionality()
                .listVolumeEncryptionKeyIds(testDto.getName(), resourceGroupForTest, instanceIds));
        testContext.getCloudProvider().verifyVolumeEncryptionKey(volumeKmsKeyIds, testContext.given(EnvironmentTestDto.class).getRequest().getName());
        return testDto;
    }

    private DistroXTestDto verifyDistroxVolumeEncryptionKey(TestContext testContext, DistroXTestDto testDto, CloudbreakClient cloudbreakClient) {
        List<String> instanceIds = distroxUtil.getInstanceIds(testDto, cloudbreakClient, MASTER.getName());
        List<String> volumeKmsKeyIds = new ArrayList<>(testContext.getCloudProvider().getCloudFunctionality()
                .listVolumeEncryptionKeyIds(testDto.getName(), resourceGroupForTest, instanceIds));
        testContext.getCloudProvider().verifyVolumeEncryptionKey(volumeKmsKeyIds, testContext.given(EnvironmentTestDto.class).getRequest().getName());
        return testDto;
    }

    private DistroXTestDto verifyPreTerminationRecipe(TestContext testContext, DistroXTestDto testDto, String cloudStorageBaseLocation, String recipeName) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageListContainer(cloudStorageBaseLocation, recipeName, false);
        return testDto;
    }
}
