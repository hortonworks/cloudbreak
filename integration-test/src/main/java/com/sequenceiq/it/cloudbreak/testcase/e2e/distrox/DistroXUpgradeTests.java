package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.COMPUTE;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.WORKER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXUpgradeTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxUpgradeTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.DistroxUtil;
import com.sequenceiq.it.cloudbreak.util.InstanceUtil;
import com.sequenceiq.it.cloudbreak.util.VolumeUtils;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxUpgradeReplaceVms;

public class DistroXUpgradeTests extends AbstractE2ETest {

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private DistroxUtil distroxUtil;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
        createEnvironmentWithNetworkAndFreeIpa(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(given = "there is a running Cloudbreak, and an environment with SDX and DistroX cluster in available state",
            when = "upgrade called on the DistroX cluster", then = "DistroX upgrade should be successful, the cluster should be up and running")
    public void testDistroXUpgrade(TestContext testContext) {
        List<String> actualVolumeIds = new ArrayList<>();
        List<String> expectedVolumeIds = new ArrayList<>();

        String sdxName = resourcePropertyProvider().getName();
        String distroXName = resourcePropertyProvider().getName();
        String currentRuntimeVersion = commonClusterManagerProperties.getUpgrade().getCurrentRuntimeVersion();
        String targetRuntimeVersion = commonClusterManagerProperties.getUpgrade().getTargetRuntimeVersion();
        testContext
                .given(sdxName, SdxTestDto.class)
                .withCloudStorage()
                .withRuntimeVersion(currentRuntimeVersion)
                .when(sdxTestClient.create(), key(sdxName))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxName))
                .awaitForInstance(getSdxInstancesHealthyState())
                .validate();
        testContext
                .given(distroXName, DistroXTestDto.class)
                .withTemplate(commonClusterManagerProperties.getInternalDistroXBlueprintName())
                .when(distroXTestClient.create(), key(distroXName))
                .await(STACK_AVAILABLE)
                .awaitForInstance(InstanceUtil.getHealthyDistroXInstances())
                .then((tc, testDto, client) -> {
                    List<String> instances = distroxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    instances.addAll(distroxUtil.getInstanceIds(testDto, client, COMPUTE.getName()));
                    instances.addAll(distroxUtil.getInstanceIds(testDto, client, WORKER.getName()));
                    CloudFunctionality cloudFunctionality = tc.getCloudProvider().getCloudFunctionality();
                    expectedVolumeIds.addAll(cloudFunctionality.listInstanceVolumeIds(instances));
                    return testDto;
                })
                .validate();
        testContext
                .given(distroXName, DistroXTestDto.class)
                .when(distroXTestClient.stop(), key(distroXName))
                .await(STACK_STOPPED)
                .validate();
        testContext
                .given(SdxUpgradeTestDto.class)
                .withReplaceVms(SdxUpgradeReplaceVms.DISABLED)
                .withRuntime(targetRuntimeVersion)
                .given(sdxName, SdxTestDto.class)
                .when(sdxTestClient.upgrade(), key(sdxName))
                .await(SdxClusterStatusResponse.DATALAKE_UPGRADE_IN_PROGRESS, key(sdxName).withWaitForFlow(Boolean.FALSE))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxName))
                .awaitForInstance(getSdxInstancesHealthyState())
                .validate();
        testContext
                .given(distroXName, DistroXTestDto.class)
                .when(distroXTestClient.start(), key(distroXName))
                .await(STACK_AVAILABLE)
                .validate();
        testContext
                .given(DistroXUpgradeTestDto.class)
                .withRuntime(targetRuntimeVersion)
                .given(distroXName, DistroXTestDto.class)
                .when(distroXTestClient.upgrade(), key(distroXName))
                .await(STACK_AVAILABLE, key(distroXName))
                .awaitForInstance(InstanceUtil.getHealthyDistroXInstances())
                .then((tc, testDto, client) -> {
                    List<String> instances = distroxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    instances.addAll(distroxUtil.getInstanceIds(testDto, client, COMPUTE.getName()));
                    instances.addAll(distroxUtil.getInstanceIds(testDto, client, WORKER.getName()));
                    CloudFunctionality cloudFunctionality = tc.getCloudProvider().getCloudFunctionality();
                    actualVolumeIds.addAll(cloudFunctionality.listInstanceVolumeIds(instances));
                    return testDto;
                })
                .then((tc, testDto, client) -> VolumeUtils.compareVolumeIdsAfterRepair(testDto, actualVolumeIds, expectedVolumeIds))
                .validate();
    }
}
