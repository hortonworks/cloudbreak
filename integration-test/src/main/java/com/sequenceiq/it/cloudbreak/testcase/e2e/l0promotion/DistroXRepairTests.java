package com.sequenceiq.it.cloudbreak.testcase.e2e.l0promotion;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.UmsTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupsBuilder;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.DistroxUtil;
import com.sequenceiq.it.cloudbreak.util.VolumeUtils;
import com.sequenceiq.it.cloudbreak.util.clouderamanager.ClouderaManagerUtil;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshJClientActions;

/**
 * Based on the [CB-15474 removed ephemeral disk tests from azure-longrunning-e2e-tests]:
 *  Mitigation plan is to have only 1 testcase with this type of instance (preferably repair due to mounting)
 *  and move it to the mow-dev test suite which will only run once a week.
 */
public class DistroXRepairTests extends AbstractE2ETest {

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private DistroxUtil distroxUtil;

    @Inject
    private ClouderaManagerUtil clouderaManagerUtil;

    @Inject
    private SshJClientActions sshJClientActions;

    @Inject
    private UmsTestClient umsTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        assertSupportedCloudPlatform(CloudPlatform.AZURE);
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
        createEnvironmentWithFreeIpaAndDatalake(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running environment with FreeIPA and SDX in available state",
            when = "a new DistroX should be created",
                and = "MASTER host group should be recovered, where the instance had been terminated",
            then = "DistroX recovery should be successful, the cluster should be up and running with same volumes"
    )
    public void testEphemeralDistroXMasterRepairWithTerminatedInstances(TestContext testContext) {
        String distrox = resourcePropertyProvider().getName();
        List<String> actualVolumeIds = new ArrayList<>();
        List<String> expectedVolumeIds = new ArrayList<>();
        String newWorkloadPassword = "Admin@123";
        String userCrn = testContext.getActingUserCrn().toString();
        String workloadUsername = testContext
                .given(UmsTestDto.class)
                .assignTarget(EnvironmentTestDto.class.getSimpleName())
                .when(umsTestClient.getUserDetails(userCrn))
                .getResponse().getWorkloadUsername();

        testContext
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.getLastSyncOperationStatus())
                .await(OperationState.COMPLETED)
                .given(UmsTestDto.class).assignTarget(EnvironmentTestDto.class.getSimpleName())
                .when(umsTestClient.setWorkloadPassword(newWorkloadPassword))
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.syncAll())
                .await(OperationState.COMPLETED)
                .given(distrox, DistroXTestDto.class)
                .withInstanceGroupsEntity(new DistroXInstanceGroupsBuilder(testContext)
                        .defaultHostGroup()
                        .withStorageOptimizedInstancetype()
                        .build())
                .when(distroXTestClient.create(), key(distrox))
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> {
                    verifyMountPointsUsedForTemporalDisks(testDto, "ephfs", "ephfs1");
                    return testDto;
                })
                .then((tc, testDto, client) -> {
                    CloudFunctionality cloudFunctionality = tc.getCloudProvider().getCloudFunctionality();
                    List<String> instancesToDelete = distroxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    expectedVolumeIds.addAll(cloudFunctionality.listInstanceVolumeIds(testDto.getName(), instancesToDelete));
                    cloudFunctionality.deleteInstances(testDto.getName(), instancesToDelete);
                    return testDto;
                })
                .awaitForHostGroup(MASTER.getName(), InstanceStatus.DELETED_ON_PROVIDER_SIDE)
                .when(distroXTestClient.repair(MASTER), key(distrox))
                .await(STACK_AVAILABLE, key(distrox))
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> {
                    verifyMountPointsUsedForTemporalDisks(testDto, "ephfs", "ephfs1");
                    return testDto;
                })
                .then((tc, testDto, client) -> clouderaManagerUtil.checkClouderaManagerYarnNodemanagerRoleConfigGroups(testDto, workloadUsername,
                        newWorkloadPassword))
                .then((tc, testDto, client) -> {
                    CloudFunctionality cloudFunctionality = tc.getCloudProvider().getCloudFunctionality();
                    List<String> instanceIds = distroxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    actualVolumeIds.addAll(cloudFunctionality.listInstanceVolumeIds(testDto.getName(), instanceIds));
                    return testDto;
                })
                .then((tc, testDto, client) -> VolumeUtils.compareVolumeIdsAfterRepair(testDto, actualVolumeIds, expectedVolumeIds))
                .validate();
    }

    private void verifyMountPointsUsedForTemporalDisks(DistroXTestDto testDto, String awsMountPrefix, String azureMountDir) {
        List<InstanceGroupV4Response> instanceGroups = testDto.getResponse().getInstanceGroups();
        if (activeCloudPlatform(CloudPlatform.AWS)) {
            sshJClientActions.checkAwsEphemeralDisksMounted(instanceGroups, List.of(HostGroupType.WORKER.getName()), awsMountPrefix);
        } else if (activeCloudPlatform(CloudPlatform.AZURE)) {
            sshJClientActions.checkAzureTemporalDisksMounted(instanceGroups, List.of(HostGroupType.WORKER.getName()), azureMountDir);
        }
    }

    private boolean activeCloudPlatform(CloudPlatform cloudPlatform) {
        return cloudPlatform.name().equalsIgnoreCase(commonCloudProperties().getCloudProvider());
    }
}
