package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox.ephemeral;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.util.SanitizerUtil;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupsBuilder;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.DistroxUtil;
import com.sequenceiq.it.cloudbreak.util.VolumeUtils;
import com.sequenceiq.it.cloudbreak.util.clouderamanager.ClouderaManagerUtil;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshJClientActions;

public class DistroXRepairTests extends AbstractE2ETest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXRepairTests.class);

    private static final String MOCK_UMS_PASSWORD = "Password123!";

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private DistroxUtil distroxUtil;

    @Inject
    private ClouderaManagerUtil clouderaManagerUtil;

    @Inject
    private SshJClientActions sshJClientActions;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
        createEnvironmentWithFreeIpaAndDatalake(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak, and an environment with SDX and DistroX cluster in available state",
            when = "recovery called on the MASTER host group of DistroX cluster, where the EC2 instance had been terminated",
            then = "DistroX recovery should be successful, the cluster should be up and running"
    )
    public void testEphemeralDistroXMasterRepairWithTerminatedEC2Instances(TestContext testContext) {
        String distrox = resourcePropertyProvider().getName();
        List<String> actualVolumeIds = new ArrayList<>();
        List<String> expectedVolumeIds = new ArrayList<>();

        String username = testContext.getActingUserCrn().getResource();
        String sanitizedUserName = SanitizerUtil.sanitizeWorkloadUsername(username);

        testContext
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
                .then((tc, testDto, client) -> clouderaManagerUtil.checkClouderaManagerYarnNodemanagerRoleConfigGroups(testDto, sanitizedUserName,
                        MOCK_UMS_PASSWORD))
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
