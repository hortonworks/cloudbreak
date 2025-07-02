package com.sequenceiq.it.cloudbreak.testcase.e2e.l0promotion;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseAvailabilityType;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretType;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaRotationTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.it.cloudbreak.util.ssh.SshJUtil;
import com.sequenceiq.it.util.imagevalidation.ImageValidatorE2ETest;
import com.sequenceiq.it.util.imagevalidation.ImageValidatorE2ETestUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;
import com.sequenceiq.sdx.rotation.DatalakeSecretType;

public class MonitoringTests extends AbstractE2ETest implements ImageValidatorE2ETest {

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private SshJUtil sshJUtil;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private ImageValidatorE2ETestUtil imageValidatorE2ETestUtil;

    @Value("${integrationtest.telemetry.remoteWriteUrl:}")
    private String remoteWriteUrl;

    @Override
    protected void setupTest(TestContext testContext) {
        imageValidatorE2ETestUtil.setupTest(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "creating an environment along with freeIpa by metering pre-warmed image",
            when = "the new environment and attached freeIpa should be created",
                and = "metering setting should be inherited from environment",
            then = "Metering Services, Scrapping and Metrics should be up and running on freeIpa")
    public void testMonitoringOnEnvironment(TestContext testContext) {
        testContext
                .given(EnvironmentNetworkTestDto.class)
                .given("telemetry", TelemetryTestDto.class)
                    .withLogging()
                    .withReportClusterLogs()
                    .withMonitoring(remoteWriteUrl)
                .given(EnvironmentTestDto.class)
                    .withNetwork()
                    .withTelemetry("telemetry")
                    .withTunnel(testContext.getTunnel())
                    .withCreateFreeIpa(Boolean.TRUE)
                    .withOneFreeIpaNode()
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.getLastSyncOperationStatus())
                .await(OperationState.COMPLETED)
                .given(FreeIpaTestDto.class)
                    .withEnvironment()
                .when(freeIpaTestClient.describe())
                .then((tc, testDto, client) -> {
                    sshJUtil.checkCommonMonitoringStatus(testDto, testDto.getEnvironmentCrn(), client,
                            List.of("node_filesystem_free_bytes"), List.of("cdp-request-signer"));
                    sshJUtil.checkFilesystemFreeBytesGeneratedMetric(testDto, testDto.getEnvironmentCrn(), client);
                    return testDto;
                })
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.describe())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "creating sequentally and separatelly environment, FreeIpa then SDX with metering pre-warmed images and no database",
            when = "when new freeIpa and SDX are up and running then new DistroX should be created also with metering pre-warmed image and no database",
                and = "metering should be tested on all the Master host groups",
            then = "Metering Services, Scrapping and Metrics should be up and running on instances")
    public void testMonitoringOnFreeIpaSdxDistrox(TestContext testContext) {
        DistroXDatabaseRequest distroXDatabaseRequest = new DistroXDatabaseRequest();
        distroXDatabaseRequest.setAvailabilityType(DistroXDatabaseAvailabilityType.NONE);
        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setCreate(false);
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NONE);

        testContext
                .given(EnvironmentNetworkTestDto.class)
                .given("telemetry", TelemetryTestDto.class)
                    .withLogging()
                    .withReportClusterLogs()
                    .withMonitoring(remoteWriteUrl)
                .given(EnvironmentTestDto.class)
                    .withNetwork()
                    .withTelemetry("telemetry")
                    .withTunnel(testContext.getTunnel())
                    .withCreateFreeIpa(Boolean.FALSE)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIpaTestDto.class)
                    .withEnvironment()
                    .withTelemetry("telemetry")
                    .withArchitecture(imageValidatorE2ETestUtil.getArchitecture(testContext).getName())
                .when(freeIpaTestClient.create())
                .await(Status.AVAILABLE)
                .awaitForHealthyInstances()
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.getLastSyncOperationStatus())
                .await(OperationState.COMPLETED)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .then((tc, testDto, client) -> sshJUtil.checkNetworkStatus(testDto, testDto.getEnvironmentCrn(), client))
                .then((tc, testDto, client) -> sshJUtil.checkFluentdStatus(testDto, testDto.getEnvironmentCrn(), client))
                .then((tc, testDto, client) -> sshJUtil.checkCdpServiceStatus(testDto, testDto.getEnvironmentCrn(), client))
                .then((tc, testDto, client) -> {
                    sshJUtil.checkCommonMonitoringStatus(testDto, testDto.getEnvironmentCrn(), client,
                            List.of("node_filesystem_free_bytes"), List.of("cdp-request-signer"));
                    sshJUtil.checkFilesystemFreeBytesGeneratedMetric(testDto, testDto.getEnvironmentCrn(), client);
                    return testDto;
                })
                .given(FreeIpaRotationTestDto.class)
                .withSecrets(List.of(FreeIpaSecretType.COMPUTE_MONITORING_CREDENTIALS))
                .when(freeIpaTestClient.rotateSecret())
                .awaitForFlow()
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.describe())
                .given(SdxInternalTestDto.class)
                    .withEnvironment()
                    .withDatabase(sdxDatabaseRequest)
                    .withTelemetry("telemetry")
                .when(sdxTestClient.createInternal())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .when(sdxTestClient.describeInternal())
                .then((tc, testDto, client) -> sshJUtil.checkNetworkStatus(testDto, testDto.getResponse().getStackV4Response().getInstanceGroups(),
                        List.of(MASTER.getName())))
                .then((tc, testDto, client) -> sshJUtil.checkFluentdStatus(testDto, testDto.getResponse().getStackV4Response().getInstanceGroups(),
                        List.of(MASTER.getName())))
                .then((tc, testDto, client) -> sshJUtil.checkCdpServiceStatus(testDto, testDto.getResponse().getStackV4Response().getInstanceGroups(),
                        List.of(MASTER.getName())))
                .then((tc, testDto, client) -> {
                    sshJUtil.checkCommonMonitoringStatus(testDto, testDto.getResponse().getStackV4Response().getInstanceGroups(),
                            List.of(MASTER.getName()), List.of("node_filesystem_free_bytes"), List.of("cdp-request-signer"));
                    sshJUtil.checkFilesystemFreeBytesGeneratedMetric(testDto, testDto.getResponse().getStackV4Response().getInstanceGroups(),
                            List.of(MASTER.getName()));
                    return testDto;
                })
                .when(sdxTestClient.rotateSecret(Set.of(DatalakeSecretType.COMPUTE_MONITORING_CREDENTIALS)))
                .awaitForFlow()
                .given(DistroXTestDto.class)
                    .withExternalDatabase(distroXDatabaseRequest)
                    .withEnvironment()
                .when(distroXTestClient.create())
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .when(distroXTestClient.get())
                .then((tc, testDto, client) -> sshJUtil.checkNetworkStatus(testDto, testDto.getResponse().getInstanceGroups(), List.of(MASTER.getName())))
                .then((tc, testDto, client) -> sshJUtil.checkFluentdStatus(testDto, testDto.getResponse().getInstanceGroups(), List.of(MASTER.getName())))
                .then((tc, testDto, client) -> sshJUtil.checkCdpServiceStatus(testDto, testDto.getResponse().getInstanceGroups(), List.of(MASTER.getName())))
                .then((tc, testDto, client) -> {
                    sshJUtil.checkCommonMonitoringStatus(testDto, testDto.getResponse().getInstanceGroups(),
                            List.of(MASTER.getName()), List.of("node_filesystem_free_bytes"), List.of("cdp-request-signer"));
                    sshJUtil.checkFilesystemFreeBytesGeneratedMetric(testDto, testDto.getResponse().getInstanceGroups(),
                            List.of(MASTER.getName()));
                    return testDto;
                })
                .when(distroXTestClient.rotateSecret(Set.of(CloudbreakSecretType.COMPUTE_MONITORING_CREDENTIALS)))
                .awaitForFlow()
                .validate();
    }
}
