package com.sequenceiq.it.cloudbreak.testcase.e2e.dbserverencryption;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseServerSslMode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseAvailabilityType;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseRequest;
import com.sequenceiq.it.cloudbreak.action.sdx.SdxDatabaseServerAction;
import com.sequenceiq.it.cloudbreak.action.v1.distrox.DistroXDatabaseServerAction;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupsBuilder;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.sdx.PreconditionSdxE2ETest;
import com.sequenceiq.it.cloudbreak.testcase.e2e.sdx.SdxUpgradeDatabaseTestUtil;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.it.cloudbreak.util.ssh.SshJUtil;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

public class GcpExternalDatabaseServerEncryptionTests extends PreconditionSdxE2ETest {

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private SdxUpgradeDatabaseTestUtil sdxUpgradeDatabaseTestUtil;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private SshJUtil sshJUtil;

    @Inject
    private SdxDatabaseServerAction sdxDatabaseServerAction;

    @Inject
    private DistroXDatabaseServerAction distroxDatabaseServerAction;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak, and an encrypted environment is created on GCP with external database",
            when = "SDX and DistroX is created with external encrypted database",
            then = "SSL encryption is enabled for GCP, the cluster should be up and running"
    )
    public void testExternalDatabaseEncryptionOnGCP(TestContext testContext) {
        String sdx = resourcePropertyProvider().getName();
        String dx = sdx + "dx";

        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NON_HA);
        sdxDatabaseRequest.setDatabaseEngineVersion(sdxUpgradeDatabaseTestUtil.getTargetMajorVersion().getMajorVersion());

        DistroXDatabaseRequest distroXDatabaseRequest = new DistroXDatabaseRequest();
        distroXDatabaseRequest.setAvailabilityType(DistroXDatabaseAvailabilityType.NON_HA);

        testContext
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                .given(EnvironmentTestDto.class)
                .withNetwork()
                .withTelemetry("telemetry")
                .withTunnel(testContext.getTunnel())
                .withResourceEncryption(Boolean.TRUE)
                .withCreateFreeIpa(Boolean.TRUE)
                .withFreeIpaNodes(getFreeIpaInstanceCountByProvider(testContext))
                .withFreeIpaImage(commonCloudProperties().getImageValidation().getFreeIpaImageCatalog(),
                        commonCloudProperties().getImageValidation().getFreeIpaImageUuid())
                .when(environmentTestClient.create())
                .awaitForCreationFlow()
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.describe())
                .given(sdx, SdxTestDto.class)
                .withCloudStorage()
                .withExternalDatabase(sdxDatabaseRequest)
                .when(sdxTestClient.create(), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForHealthyInstances()
                .given(sdx, SdxTestDto.class)
                .when(sdxTestClient.describe())
                .then((tc, testDto, client) -> {
                    validateEncryption(testDto, tc, client);
                    validateAz(testDto.getResponse().getStackV4Response().getInstanceGroups(), tc, testDto.getResponse().getName());
                    return testDto;
                })
                .given(dx, DistroXTestDto.class)
                .withTemplate(commonClusterManagerProperties().getDataMartDistroXBlueprintNameForCurrentRuntime())
                .withExternalDatabase(distroXDatabaseRequest)
                .withInstanceGroupsEntity(new DistroXInstanceGroupsBuilder(testContext)
                    .verticalScaleHostGroup()
                    .build())
                .when(distroXTestClient.create(), key("dx"))
                .await(STACK_AVAILABLE, key("dx"))
                .awaitForHealthyInstances()
                .given(dx, DistroXTestDto.class)
                .when(distroXTestClient.get(), key(dx))
                .then((tc, testDto, client) -> {
                    validateDistroXEncryption(testDto, tc, client);
                    validateAz(testDto.getResponse().getInstanceGroups(), tc, testDto.getResponse().getName());
                    return testDto;
                })
                .validate();
    }

    private void validateEncryption(SdxTestDto sdxTestDto, TestContext tc, SdxClient sdxClient) throws Exception {
        SdxClusterDetailResponse sdxClusterDetailResponse = sdxTestDto.getResponse();
        String clusterName = sdxClusterDetailResponse.getName();
        StackDatabaseServerResponse stackDatabaseServerResponse = sdxDatabaseServerAction.getSdxExternalDatabaseConfigs(sdxClusterDetailResponse.getCrn(),
                sdxClient, tc);
        validateDbServer(stackDatabaseServerResponse, clusterName);

        List<InstanceGroupV4Response> instanceGroups = sdxClusterDetailResponse.getStackV4Response().getInstanceGroups();
        validateDbSslMode(instanceGroups);
    }

    private static void validateDbServer(StackDatabaseServerResponse stackDatabaseServerResponse, String clusterName) {
        if (!DatabaseServerSslMode.ENABLED.equals(stackDatabaseServerResponse.getSslConfig().getSslMode())) {
            throw new TestFailException("SSL Encryption is not enabled for external database for cluster : " + clusterName);
        }
    }

    private void validateDistroXEncryption(DistroXTestDto distroxTestDto, TestContext tc, CloudbreakClient cloudbreakClient) throws Exception {
        StackV4Response distroxResponse = distroxTestDto.getResponse();
        String clusterName = distroxResponse.getName();
        StackDatabaseServerResponse stackDatabaseServerResponse = distroxDatabaseServerAction.getExternalDatabaseConfigs(distroxTestDto.getCrn(),
                cloudbreakClient, distroxTestDto.getTestContext());
        validateDbServer(stackDatabaseServerResponse, clusterName);

        List<InstanceGroupV4Response> instanceGroups = distroxResponse.getInstanceGroups();
        validateDbSslMode(instanceGroups);
    }

    private void validateDbSslMode(List<InstanceGroupV4Response> instanceGroups) {
        Map<String, Pair<Integer, String>> dbSslModeConnectionMap = sshJUtil.getSSLModeForExternalDBByIp(instanceGroups, List.of("master"),
                commonCloudProperties().getDefaultPrivateKeyFile());

        for (String ip : dbSslModeConnectionMap.keySet()) {
            Pair<Integer, String> connectionUrlPair = dbSslModeConnectionMap.get(ip);
            if (!connectionUrlPair.getValue().contains("verify-ca")) {
                throw new TestFailException("Connection URL for external database has SSL Mode set to 'verify-full' for instance ip: " + ip);
            }
        }
    }

    private void validateAz(List<InstanceGroupV4Response> instanceGroups, TestContext tc, String clusterName) {
        Map<String, String> instanceZoneMap = instanceGroups.stream()
                .map(ig -> ig.getMetadata())
                .filter(Objects::nonNull)
                .flatMap(ins -> ins.stream())
                .collect(Collectors.toMap(InstanceMetaDataV4Response::getInstanceId, InstanceMetaDataV4Response::getAvailabilityZone));
        Map<String, String> availabilityZoneForVms = getCloudFunctionality(tc).listAvailabilityZonesForVms(clusterName, instanceZoneMap);
        List<String> instancesWithNoAz = availabilityZoneForVms.entrySet().stream()
                .filter(entry -> StringUtils.isEmpty(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(instancesWithNoAz)) {
            throw new TestFailException(String.format("Availability Zone is missing for instances %s in %s",
                    instancesWithNoAz.stream().collect(Collectors.joining(",")), clusterName));
        }
        Set<String> zones = availabilityZoneForVms.values().stream().collect(Collectors.toSet());
        if (zones.size() > 1) {
            throw new TestFailException(String.format("There are multiple Availability zones %s for instances in %s",
                    zones.stream().collect(Collectors.joining(",")), clusterName));
        }
    }

    protected CloudFunctionality getCloudFunctionality(TestContext testContext) {
        return testContext.getCloudProvider().getCloudFunctionality();
    }
}
