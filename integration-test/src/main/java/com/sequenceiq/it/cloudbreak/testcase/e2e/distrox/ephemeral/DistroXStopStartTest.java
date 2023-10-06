package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox.ephemeral;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupsBuilder;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.clouderamanager.ClouderaManagerUtil;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;

public class DistroXStopStartTest extends AbstractE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXStopStartTest.class);

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private ClouderaManagerUtil clouderaManagerUtil;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
        createDefaultDatalake(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running cloudbreak",
            when = "two valid DistroX create requests are sent one with ephemeral temporary storage and one without it",
            then = "clusters are created, device mount point are checked, and after stopping and starting the clusters ephemeral store handling checked again")
    public void testCreateDistroXWithEphemeralTemporaryStorage(TestContext testContext) {

        testContext
                .given("non_eph_dx", DistroXTestDto.class)
                .withInstanceGroupsEntity(new DistroXInstanceGroupsBuilder(testContext)
                        .defaultHostGroup()
                        .build())
                .when(distroXTestClient.create(), RunningParameter.key("non_eph_dx"))
                .given("eph_dx", DistroXTestDto.class)
                .withInstanceGroupsEntity(new DistroXInstanceGroupsBuilder(testContext)
                        .defaultHostGroup()
                        .withStorageOptimizedInstancetype()
                        .build())
                .when(distroXTestClient.create(), RunningParameter.key("eph_dx"))
                .given("non_eph_dx", DistroXTestDto.class)
                .await(STACK_AVAILABLE, RunningParameter.key("non_eph_dx"))
                .then((tc, testDto, client) -> verifyEphemeralVolumesShouldNotBeConfiguredInHdfs(tc, testDto))
                .when(distroXTestClient.stop(), RunningParameter.key("non_eph_dx"))
                .await(STACK_STOPPED, RunningParameter.key("non_eph_dx"))
                .when(distroXTestClient.start(), RunningParameter.key("non_eph_dx"))
                .await(STACK_AVAILABLE, RunningParameter.key("non_eph_dx"))
                .then((tc, testDto, client) -> verifyEphemeralVolumesShouldNotBeConfiguredInHdfs(tc, testDto))
                .given("eph_dx", DistroXTestDto.class)
                .await(STACK_AVAILABLE, RunningParameter.key("eph_dx"))
                .then(this::verifyMountedDisks)
                .then((tc, testDto, client) -> verifyEphemeralVolumesShouldNotBeConfiguredInHdfs(tc, testDto))
                .then((tc, testDto, client) -> clouderaManagerUtil.checkClouderaManagerYarnNodemanagerRoleConfigGroups(testDto, tc))
                .when(distroXTestClient.stop(), RunningParameter.key("eph_dx"))
                .await(STACK_STOPPED, RunningParameter.key("eph_dx"))
                .when(distroXTestClient.start(), RunningParameter.key("eph_dx"))
                .await(STACK_AVAILABLE, RunningParameter.key("eph_dx"))
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

    private DistroXTestDto verifyMountedDisks(TestContext testContext, DistroXTestDto testDto, CloudbreakClient cloudbreakClient) {
        CloudFunctionality cloudFunctionality = testContext.getCloudProvider().getCloudFunctionality();
        List<InstanceGroupV4Response> instanceGroups = testDto.getResponse().getInstanceGroups();
        cloudFunctionality.checkMountedDisks(instanceGroups, List.of(HostGroupType.WORKER.getName()));
        return testDto;
    }
}