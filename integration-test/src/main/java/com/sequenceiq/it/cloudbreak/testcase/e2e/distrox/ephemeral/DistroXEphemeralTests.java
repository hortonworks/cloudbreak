package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox.ephemeral;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXUpgradeTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupsBuilder;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.DistroxUtil;
import com.sequenceiq.it.cloudbreak.util.VolumeUtils;
import com.sequenceiq.it.cloudbreak.util.clouderamanager.ClouderaManagerUtil;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class DistroXEphemeralTests extends AbstractE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXEphemeralTests.class);

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private ClouderaManagerUtil clouderaManagerUtil;

    @Inject
    private DistroxUtil distroxUtil;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Inject
    private SdxTestClient sdxTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
        createEnvironmentWithFreeIpa(testContext);

        boolean govCloud = testContext.getCloudProvider().getGovCloud();
        String currentUpgradeRuntimeVersion = commonClusterManagerProperties.getUpgrade().getDistroXUpgradeCurrentVersion(govCloud);
        createAndWaitDatalakeWithRuntime(testContext, currentUpgradeRuntimeVersion);
        createDataHubWithStorageOptimizedInstancesAndWithRuntime(testContext, currentUpgradeRuntimeVersion);
        waitForDatahubCreation(testContext);
    }

    private void createAndWaitDatalakeWithRuntime(TestContext testContext, String currentRuntimeVersion) {
        testContext
                .given(SdxTestDto.class)
                .withCloudStorage()
                .withRuntimeVersion(currentRuntimeVersion)
                .when(sdxTestClient.create())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .validate();
    }

    private void createDataHubWithStorageOptimizedInstancesAndWithRuntime(TestContext testContext, String currentRuntimeVersion) {
        testContext
                .given(DistroXTestDto.class)
                .withTemplate(commonClusterManagerProperties.getDataEngDistroXBlueprintName(currentRuntimeVersion))
                .withInstanceGroupsEntity(new DistroXInstanceGroupsBuilder(testContext)
                        .defaultHostGroup()
                        .withStorageOptimizedInstancetype()
                        .build())
                .when(distroXTestClient.create())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "a valid DistroX with ephemeral temporary storage",
            when = "stopping and starting the cluster",
            then = "clusters is available, device mount point are checked, and after stopping and starting the cluster ephemeral store handling checked again")
    public void testStopStartDistroXWithEphemeralTemporaryStorage(TestContext testContext) {
        testContext
                .given(DistroXTestDto.class)
                .await(STACK_AVAILABLE)
                .then(this::verifyMountedDisks)
                .then((tc, testDto, client) -> verifyEphemeralVolumesShouldNotBeConfiguredInHdfs(tc, testDto))
                .then((tc, testDto, client) -> clouderaManagerUtil.checkClouderaManagerYarnNodemanagerRoleConfigGroups(testDto, tc))
                .when(distroXTestClient.stop())
                .await(STACK_STOPPED)
                .when(distroXTestClient.start())
                .await(STACK_AVAILABLE)
                .then(this::verifyMountedDisks)
                .then((tc, testDto, client) -> clouderaManagerUtil.checkClouderaManagerYarnNodemanagerRoleConfigGroups(testDto, tc))
                .validate();
    }

    private DistroXTestDto verifyEphemeralVolumesShouldNotBeConfiguredInHdfs(TestContext testContext, DistroXTestDto testDto) {
        CloudFunctionality cloudFunctionality = testContext.getCloudProvider().getCloudFunctionality();
        List<InstanceGroupV4Response> instanceGroups = testDto.getResponse().getInstanceGroups();
        Set<String> mountPoints = cloudFunctionality.getVolumeMountPoints(instanceGroups, List.of(HostGroupType.WORKER.getName()));

        clouderaManagerUtil.checkClouderaManagerHdfsDatanodeRoleConfigGroups(testDto, testContext, mountPoints);
        clouderaManagerUtil.checkClouderaManagerHdfsNamenodeRoleConfigGroups(testDto, testContext, mountPoints);
        return testDto;
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak, and an environment with SDX and DistroX cluster in available state",
            when = "recovery called on the MASTER host group of DistroX cluster, where the EC2 instance had been terminated",
            then = "DistroX recovery should be successful, the cluster should be up and running"
    )
    public void testEphemeralDistroXMasterRepairWithTerminatedEC2Instances(TestContext testContext) {
        List<String> actualVolumeIds = new ArrayList<>();
        List<String> expectedVolumeIds = new ArrayList<>();

        testContext
                .given(DistroXTestDto.class)
                .then(this::verifyMountedDisks)
                .then((tc, testDto, client) -> {
                    CloudFunctionality cloudFunctionality = tc.getCloudProvider().getCloudFunctionality();
                    List<String> instancesToDelete = distroxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    expectedVolumeIds.addAll(cloudFunctionality.listInstancesVolumeIds(testDto.getName(), instancesToDelete));
                    cloudFunctionality.deleteInstances(testDto.getName(), instancesToDelete);
                    return testDto;
                })
                .awaitForHostGroup(MASTER.getName(), InstanceStatus.DELETED_ON_PROVIDER_SIDE)
                .when(distroXTestClient.repair(MASTER))
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .then(this::verifyMountedDisks)
                .then((tc, testDto, client) -> clouderaManagerUtil.checkClouderaManagerYarnNodemanagerRoleConfigGroups(testDto, tc))
                .then((tc, testDto, client) -> {
                    CloudFunctionality cloudFunctionality = tc.getCloudProvider().getCloudFunctionality();
                    List<String> instanceIds = distroxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    actualVolumeIds.addAll(cloudFunctionality.listInstancesVolumeIds(testDto.getName(), instanceIds));
                    return testDto;
                })
                .then((tc, testDto, client) -> VolumeUtils.compareVolumeIdsAfterRepair(testDto, actualVolumeIds, expectedVolumeIds))
                .validate();
    }

    private DistroXTestDto verifyMountedDisks(TestContext testContext, DistroXTestDto testDto, CloudbreakClient cloudbreakClient) {
        CloudFunctionality cloudFunctionality = testContext.getCloudProvider().getCloudFunctionality();
        List<InstanceGroupV4Response> instanceGroups = testDto.getResponse().getInstanceGroups();
        cloudFunctionality.checkMountedDisks(instanceGroups, List.of(HostGroupType.WORKER.getName()));
        return testDto;
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(given = "there is a running Cloudbreak, and an environment with SDX and DistroX cluster in available state",
            when = "upgrade called on the DistroX cluster", then = "DistroX upgrade should be successful, the cluster should be up and running")
    public void testDistroXEphemeralUpgrade(TestContext testContext) {
        String targetRuntimeVersion = commonClusterManagerProperties.getUpgrade().getDistroXUpgradeTargetVersion();
        testContext
                .given(DistroXUpgradeTestDto.class)
                .withRuntime(targetRuntimeVersion)
                .given(DistroXTestDto.class)
                .when(distroXTestClient.upgrade())
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> {
                    CloudFunctionality cloudFunctionality = tc.getCloudProvider().getCloudFunctionality();
                    List<InstanceGroupV4Response> instanceGroups = testDto.getResponse().getInstanceGroups();
                    cloudFunctionality.checkMountedDisks(instanceGroups, List.of(HostGroupType.WORKER.getName()));
                    return testDto;
                })
                .then((tc, testDto, client) -> clouderaManagerUtil.checkClouderaManagerYarnNodemanagerRoleConfigGroupsDirect(testDto, tc))
                .validate();
    }
}
