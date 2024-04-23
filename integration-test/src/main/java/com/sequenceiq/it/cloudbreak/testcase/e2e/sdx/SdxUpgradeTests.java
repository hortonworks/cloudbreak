package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.IDBROKER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.assertion.audit.DatalakeAuditGrpcServiceAssertion;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.it.cloudbreak.util.VolumeUtils;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

public class SdxUpgradeTests extends PreconditionSdxE2ETest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SdxUpgradeTests.class);

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private SdxUtil sdxUtil;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Inject
    private DatalakeAuditGrpcServiceAssertion datalakeAuditGrpcServiceAssertion;

    @Inject
    private SdxUpgradeDatabaseTestUtil sdxUpgradeDatabaseTestUtil;

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak, and an SDX cluster in available state",
            when = "upgrade called on the SDX cluster",
            then = "SDX upgrade should be successful, the cluster should be up and running"
    )
    public void testSDXUpgrade(TestContext testContext) {
        String sdx = resourcePropertyProvider().getName();

        List<String> actualVolumeIds = new ArrayList<>();
        List<String> expectedVolumeIds = new ArrayList<>();

        testContext
                .given(sdx, SdxTestDto.class)
                .withCloudStorage()
                .withRuntimeVersion(commonClusterManagerProperties.getUpgrade().getCurrentRuntimeVersion())
                .withExternalDatabase(sdxDbRequest())
                .when(sdxTestClient.create(), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> {
                    List<String> instances = sdxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    instances.addAll(sdxUtil.getInstanceIds(testDto, client, IDBROKER.getName()));
                    expectedVolumeIds.addAll(getCloudFunctionality(tc).listInstancesVolumeIds(testDto.getName(), instances));
                    return testDto;
                })
                .when(sdxTestClient.upgrade(), key(sdx))
                .await(SdxClusterStatusResponse.DATALAKE_UPGRADE_IN_PROGRESS, key(sdx).withWaitForFlow(Boolean.FALSE))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> {
                    List<String> instanceIds = sdxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    instanceIds.addAll(sdxUtil.getInstanceIds(testDto, client, IDBROKER.getName()));
                    actualVolumeIds.addAll(getCloudFunctionality(tc).listInstancesVolumeIds(testDto.getName(), instanceIds));
                    return testDto;
                })
                .then((tc, testDto, client) -> VolumeUtils.compareVolumeIdsAfterRepair(testDto, actualVolumeIds, expectedVolumeIds))
                .then((tc, testDto, client) -> sdxUpgradeDatabaseTestUtil.checkEmbeddedDatabaseVersionFromMasterNode(tc, testDto))
                // This assertion is disabled until the Audit Service is not configured.
                //.then(datalakeAuditGrpcServiceAssertion::upgradeClusterByNameInternal)
                .validate();
    }

    private SdxDatabaseRequest sdxDbRequest() {
        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NONE);
        sdxDatabaseRequest.setDatabaseEngineVersion(commonClusterManagerProperties.getUpgradeDatabaseServer().getOriginalEmbeddedDbSdxVersion());
        return sdxDatabaseRequest;
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak, and a HA SDX cluster in available state",
            when = "upgrade called on the HA SDX cluster",
            then = "HA SDX upgrade should be successful, the cluster should be up and running"
    )
    public void testSDXHAUpgrade(TestContext testContext) {
        String sdx = resourcePropertyProvider().getName();

        List<String> actualVolumeIds = new ArrayList<>();
        List<String> expectedVolumeIds = new ArrayList<>();

        testContext
                .given(sdx, SdxTestDto.class)
                .withClusterShape(SdxClusterShape.MEDIUM_DUTY_HA)
                .withCloudStorage()
                .withRuntimeVersion(commonClusterManagerProperties.getUpgrade().getCurrentHARuntimeVersion())
                .when(sdxTestClient.create(), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> {
                    List<String> instances = sdxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    instances.addAll(sdxUtil.getInstanceIds(testDto, client, IDBROKER.getName()));
                    expectedVolumeIds.addAll(getCloudFunctionality(tc).listInstancesVolumeIds(testDto.getName(), instances));
                    return testDto;
                })
                .when(sdxTestClient.upgrade(), key(sdx))
                .await(SdxClusterStatusResponse.DATALAKE_UPGRADE_IN_PROGRESS, key(sdx).withWaitForFlow(Boolean.FALSE))
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> {
                    List<String> instanceIds = sdxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    instanceIds.addAll(sdxUtil.getInstanceIds(testDto, client, IDBROKER.getName()));
                    actualVolumeIds.addAll(getCloudFunctionality(tc).listInstancesVolumeIds(testDto.getName(), instanceIds));
                    return testDto;
                })
                .then((tc, testDto, client) -> VolumeUtils.compareVolumeIdsAfterRepair(testDto, actualVolumeIds, expectedVolumeIds))
                // This assertion is disabled until the Audit Service is not configured.
                //.then(datalakeAuditGrpcServiceAssertion::upgradeClusterByNameInternal)
                .validate();
    }
}
