package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.util.SanitizerUtil;
import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupsBuilder;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.clouderamanager.ClouderaManagerUtil;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshJClientActions;

public class DistroXEphemeralTemporaryStorageStopStartTest extends AbstractE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXEphemeralTemporaryStorageStopStartTest.class);

    private static final String MOCK_UMS_PASSWORD = "Password123!";

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private TestParameter testParameter;

    @Inject
    private ClouderaManagerUtil clouderaManagerUtil;

    @Inject
    private SshJClientActions sshJClientActions;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
        createEnvironmentWithFreeIpaAndDatalake(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running cloudbreak",
            when = "two valid DistroX create requests are sent one with ephemeral temporary storage and one without it",
            then = "clusters are created, device mount point are checked, and after stopping and starting the clusters ephemeral store handling checked again")
    public void testCreateDistroXWithEphemeralTemporaryStorage(TestContext testContext) {

        String username = testContext.getActingUserCrn().getResource();
        String sanitizedUserName = SanitizerUtil.sanitizeWorkloadUsername(username);

        testContext
                .given("non_eph_dx", DistroXTestDto.class)
                .when(distroXTestClient.create(), RunningParameter.key("non_eph_dx"))
                .given("eph_dx", DistroXTestDto.class)
                .withInstanceGroupsEntity(new DistroXInstanceGroupsBuilder(testContext)
                        .defaultHostGroup()
                        .withEphemeralTemporaryStorage()
                        .withStorageOptimizedInstancetype()
                        .build())
                .when(distroXTestClient.create(), RunningParameter.key("eph_dx"))
                .given("non_eph_dx", DistroXTestDto.class)
                .await(STACK_AVAILABLE, RunningParameter.key("non_eph_dx"))
                .then((tc, testDto, client) -> {
                    if (testDto.getCloudPlatform() == CloudPlatform.AWS) {
                        sshJClientActions.checkNoEphemeralDisksMounted(testDto.getResponse().getInstanceGroups(), List.of(HostGroupType.WORKER.getName()));
                    }
                    return testDto;
                })
                .when(distroXTestClient.stop(), RunningParameter.key("non_eph_dx"))
                .await(STACK_STOPPED, RunningParameter.key("non_eph_dx"))
                .when(distroXTestClient.start(), RunningParameter.key("non_eph_dx"))
                .await(STACK_AVAILABLE, RunningParameter.key("non_eph_dx"))
                .then((tc, testDto, client) -> {
                    if (testDto.getCloudPlatform() == CloudPlatform.AWS) {
                        sshJClientActions.checkNoEphemeralDisksMounted(testDto.getResponse().getInstanceGroups(), List.of(HostGroupType.WORKER.getName()));
                    }
                    return testDto;
                })
                .given("eph_dx", DistroXTestDto.class)
                .await(STACK_AVAILABLE, RunningParameter.key("eph_dx"))
                .then((tc, testDto, client) -> {
                    if (testDto.getCloudPlatform() == CloudPlatform.AWS) {
                        sshJClientActions.checkEphemeralDisksMounted(testDto.getResponse().getInstanceGroups(), List.of(HostGroupType.WORKER.getName()));
                    }
                    return testDto;
                })
                .then((tc, testDto, client) -> clouderaManagerUtil.checkClouderaManagerYarnNodemanagerRoleConfigGroups(testDto, sanitizedUserName,
                        MOCK_UMS_PASSWORD))
                .when(distroXTestClient.stop(), RunningParameter.key("eph_dx"))
                .await(STACK_STOPPED, RunningParameter.key("eph_dx"))
                .when(distroXTestClient.start(), RunningParameter.key("eph_dx"))
                .await(STACK_AVAILABLE, RunningParameter.key("eph_dx"))
                .then((tc, testDto, client) -> {
                    if (testDto.getCloudPlatform() == CloudPlatform.AWS) {
                        sshJClientActions.checkEphemeralDisksMounted(testDto.getResponse().getInstanceGroups(), List.of(HostGroupType.WORKER.getName()));
                    }
                    return testDto;
                })
                .then((tc, testDto, client) -> clouderaManagerUtil.checkClouderaManagerYarnNodemanagerRoleConfigGroups(testDto, sanitizedUserName,
                        MOCK_UMS_PASSWORD))
                .validate();
    }
}