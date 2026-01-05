package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import static com.sequenceiq.it.cloudbreak.assertion.distrox.DistroXExternalDatabaseAssertion.validateTemplateContainsExternalDatabaseHostname;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.testcase.AbstractMinimalTest.STACK_AVAILABLE;
import static com.sequenceiq.it.cloudbreak.testcase.AbstractMinimalTest.STACK_DELETED;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.it.cloudbreak.assertion.distrox.AwsAvailabilityZoneAssertion;
import com.sequenceiq.it.cloudbreak.assertion.salt.SaltHighStateDurationAssertions;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.it.cloudbreak.util.DistroxUtil;
import com.sequenceiq.it.cloudbreak.util.FreeIpaInstanceUtil;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.it.cloudbreak.util.azure.AzureCloudFunctionality;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@Component
public class EncryptedTestUtil {
    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

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
    private SaltHighStateDurationAssertions saltHighStateDurationAssertions;

    public void createEnvironment(TestContext testContext) {
        testContext
                .given(EnvironmentNetworkTestDto.class)
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                .given(EnvironmentTestDto.class)
                .withNetwork()
                .withResourceEncryption(Boolean.TRUE)
                .withTelemetry("telemetry")
                .withTunnel(testContext.getTunnel())
                .withCreateFreeIpa(Boolean.FALSE)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE);
    }

    public void deleteDatahub(TestContext testContext, String preTerminationRecipeName, String baseLocationForPreTermination) {
        testContext.given(DistroXTestDto.class)
                .when(distroXTestClient.delete())
                .await(STACK_DELETED)
                .then((tc, testDto, client) -> verifyPreTerminationRecipe(tc, testDto, baseLocationForPreTermination, preTerminationRecipeName))
                .validate();
    }

    public void assertDatahub(TestContext testContext, String resourceGroupForTest) {
        testContext.given(DistroXTestDto.class)
                .then(saltHighStateDurationAssertions::saltHighStateDurationLimits)
                .then((tc, dto, cl) -> {
                    verifyDistroxVolumeEncryptionKey(tc, dto, cl, resourceGroupForTest);
                    return dto;
                })
                .then(this::verifyAwsEnaDriver)
                .then(new AwsAvailabilityZoneAssertion())
                .then(validateTemplateContainsExternalDatabaseHostname());
    }

    public void assertDatahubWithName(TestContext testContext, String resourceGroupForTest, String datahubName) {
        testContext.given(datahubName, DistroXTestDto.class)
                .then(saltHighStateDurationAssertions::saltHighStateDurationLimits)
                .then((tc, dto, cl) -> {
                    verifyDistroxVolumeEncryptionKey(tc, dto, cl, resourceGroupForTest);
                    return dto;
                }, key(datahubName))
                .then(this::verifyAwsEnaDriver, key(datahubName))
                .then(new AwsAvailabilityZoneAssertion(), key(datahubName));
                //.then(validateTemplateContainsExternalDatabaseHostname(), key(datahubName));
    }

    public void createDatahub(TestContext testContext, DistroXDatabaseRequest distroXDatabaseRequest,
            List<DistroXInstanceGroupTestDto> distroXInstanceGroupTestDtos) {
        testContext.given(DistroXTestDto.class)
                .withInstanceGroupsEntity(distroXInstanceGroupTestDtos)
                .withExternalDatabase(distroXDatabaseRequest)
                .when(distroXTestClient.create())
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances();
    }

    public void assertDatalake(TestContext testContext, String resourceGroupForTest) {
        testContext
                .given(SdxTestDto.class)
                .then((tc, dto, cl) -> {
                    verifySdxVolumeEncryptionKey(tc, dto, cl, resourceGroupForTest);
                    return dto;
                });
    }

    public SdxTestDto createDatalake(TestContext testContext) {
        return testContext.given(SdxTestDto.class)
                .withCloudStorage()
                .withEnableMultiAz()
                .when(sdxTestClient.create())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances();
    }

    public void assertEnvironmentAndFreeipa(TestContext testContext, String resourceGroupForTest) {
        testContext.given(EnvironmentTestDto.class)
                .when(environmentTestClient.describe())
                .then(this::verifyEnvironmentResponseDiskEncryptionKey)
                .given(FreeIpaTestDto.class)
                .then((tc, dto, cl) -> {
                    verifyFreeIpaVolumeEncryptionKey(tc, dto, cl, resourceGroupForTest);
                    return dto;
                });
    }

    public void doFreeipUserSync(TestContext testContext) {
        testContext.given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.getLastSyncOperationStatus())
                .await(OperationState.COMPLETED);
    }

    public void createFreeipa(TestContext testContext, CommonCloudProperties commonCloudProperties) {
        testContext.given(FreeIpaTestDto.class)
                .withEnvironment()
                .withTelemetry("telemetry")
                .when(freeIpaTestClient.create())
                .awaitForCreationFlow()
                .awaitForHealthyInstances();
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

    private FreeIpaTestDto verifyFreeIpaVolumeEncryptionKey(TestContext testContext, FreeIpaTestDto testDto, FreeIpaClient freeIpaClient,
            String resourceGroupForTest) {
        List<String> instanceIds = freeIpaInstanceUtil.getInstanceIds(testDto, freeIpaClient, MASTER.getName());
        List<String> volumeKmsKeyIds = new ArrayList<>(testContext.getCloudProvider().getCloudFunctionality()
                .listVolumeEncryptionKeyIds(testDto.getName(), resourceGroupForTest, instanceIds));
        testContext.getCloudProvider().verifyVolumeEncryptionKey(volumeKmsKeyIds, testContext.given(EnvironmentTestDto.class).getRequest().getName());
        return testDto;
    }

    private SdxTestDto verifySdxVolumeEncryptionKey(TestContext testContext, SdxTestDto testDto, SdxClient sdxClient,
            String resourceGroupForTest) {
        List<String> instanceIds = sdxUtil.getInstanceIds(testDto, sdxClient, MASTER.getName());
        List<String> volumeKmsKeyIds = new ArrayList<>(testContext.getCloudProvider().getCloudFunctionality()
                .listVolumeEncryptionKeyIds(testDto.getName(), resourceGroupForTest, instanceIds));
        testContext.getCloudProvider().verifyVolumeEncryptionKey(volumeKmsKeyIds, testContext.given(EnvironmentTestDto.class).getRequest().getName());
        return testDto;
    }

    private DistroXTestDto verifyDistroxVolumeEncryptionKey(TestContext testContext, DistroXTestDto testDto, CloudbreakClient cloudbreakClient,
            String resourceGroupForTest) {
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
