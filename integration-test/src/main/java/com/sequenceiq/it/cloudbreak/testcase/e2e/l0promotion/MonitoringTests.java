package com.sequenceiq.it.cloudbreak.testcase.e2e.l0promotion;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.testng.annotations.Test;

import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseAvailabilityType;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.IdbmmsTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXImageTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;
import com.sequenceiq.it.cloudbreak.dto.idbmms.IdbmmsTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.it.cloudbreak.util.ssh.SshJUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

/**
 * Creating 7.2.15+ pre-warmed images with Common Monitoring is still in progress.
 * So in the meantime we can use older images (7.2.12) where some features
 * (smon-exporter, cm_health_check_info and request-signer's error_count with success_count)
 * are not working properly or at all (metrics endpoint).
 *
 * In these circumstances we cannot add these test cases to the L0 Promotion suite yet. So
 * these are prepared but not finished for the L0 Monitoring test cases. In the future we can
 * extend and improve these verifications based on the finalized monitoring feature with the
 * latest images.
 */
public class MonitoringTests extends AbstractE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitoringTests.class);

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private IdbmmsTestClient idbmmsTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private SshJUtil sshJUtil;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Value("${integrationtest.telemetry.remoteWriteUrl:}")
    private String remoteWriteUrl;

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
            given = "creating an environment along with freeIpa by metering pre-warmed image",
            when = "the new environment and attached freeIpa should be created",
                and = "metering setting should be inherited from environment",
            then = "Metering Services, Scrapping and Metrics should be up and running on freeIpa")
    public void testMoniotoringOnEnvironment(TestContext testContext) {
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
                    .withFreeIpaImage(commonCloudProperties().getImageValidation().getFreeIpaImageCatalog(),
                        commonCloudProperties().getImageValidation().getFreeIpaImageUuid())
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .init(IdbmmsTestDto.class)
                .when(idbmmsTestClient.get())
                .then((tc, testDto, client) -> {
                    LOGGER.info("IDBMMS version: {}", testDto.getMappingsDetails().getMappingsVersion());
                    LOGGER.info("IDBMMS actor and group mappings: {}", testDto.getMappingsDetails().getMappingsMap());
                    LOGGER.info("IDBMMS Data Access role: {}", testDto.getMappingsDetails().getDataAccessRole());
                    LOGGER.info("IDBMMS Data Access role: {}", testDto.getMappingsDetails().getBaselineRole());
                    return testDto;
                })
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.getLastSyncOperationStatus())
                .await(OperationState.COMPLETED)
                .given(FreeIpaTestDto.class)
                    .withEnvironment()
                .when(freeIpaTestClient.describe())
                .then((tc, testDto, client) ->
                        sshJUtil.checkCommonMonitoringStatus(testDto, testDto.getEnvironmentCrn(), client,
                        List.of("node_filesystem_free_bytes"), List.of("cdp-request-signer")))
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.describe())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT, enabled = false)
    @UseSpotInstances
    @Description(
            given = "creating sequentally and separatelly environment, FreeIpa then SDX with metering pre-warmed images and no database",
            when = "when new freeIpa and SDX are up and running then new DistroX should be created also with metering pre-warmed image and no database",
                and = "metering should be tested on all the Master host groups",
            then = "Metering Services, Scrapping and Metrics should be up and running on instances")
    public void testMoniotoringOnFreeIpaSdxDistrox(TestContext testContext) {
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
                .init(IdbmmsTestDto.class)
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
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .then((tc, testDto, client) -> sshJUtil.checkCommonMonitoringStatus(testDto, testDto.getEnvironmentCrn(), client,
                        List.of("node_filesystem_free_bytes"), List.of("cdp-request-signer")))
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.describe())
                .given(SdxInternalTestDto.class)
                    .withEnvironment()
                    .withDatabase(sdxDatabaseRequest)
                    .withImageCatalogNameAndImageId(commonCloudProperties().getImageValidation().getSourceCatalogName(),
                        commonCloudProperties().getImageValidation().getImageUuid())
                .when(sdxTestClient.createInternal())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .when(sdxTestClient.describeInternal())
                .then((tc, testDto, client) -> sshJUtil.checkCommonMonitoringStatus(testDto, testDto.getResponse().getStackV4Response().getInstanceGroups(),
                        List.of(MASTER.getName()), List.of("node_filesystem_free_bytes", "cm_health_check_info"), List.of("cdp-request-signer",
                                "smon-exporter", "cm_health_check_info")))
                .given(DistroXImageTestDto.class)
                    .withImageId(commonCloudProperties().getImageValidation().getImageUuid())
                    .withImageCatalog(commonCloudProperties().getImageValidation().getSourceCatalogName())
                .given(DistroXTestDto.class)
                    .withExternalDatabase(distroXDatabaseRequest)
                    .withEnvironment()
                    .withImageSettings()
                .when(distroXTestClient.create())
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .when(distroXTestClient.get())
                .then((tc, testDto, client) -> sshJUtil.checkCommonMonitoringStatus(testDto, testDto.getResponse().getInstanceGroups(),
                        List.of(MASTER.getName()), List.of("node_filesystem_free_bytes", "cm_health_check_info"), List.of("cdp-request-signer",
                                "smon-exporter", "cm_health_check_info")))
                .validate();
    }
}
