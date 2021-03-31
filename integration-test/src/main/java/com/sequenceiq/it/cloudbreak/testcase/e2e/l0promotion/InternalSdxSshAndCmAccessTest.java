package com.sequenceiq.it.cloudbreak.testcase.e2e.l0promotion;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;

import java.util.List;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.UmsTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.sdx.PreconditionSdxE2ETest;
import com.sequenceiq.it.cloudbreak.util.clouderamanager.ClouderaManagerUtil;
import com.sequenceiq.it.cloudbreak.util.ssh.SshJUtil;

public class InternalSdxSshAndCmAccessTest extends PreconditionSdxE2ETest {

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private UmsTestClient umsTestClient;

    @Inject
    private SshJUtil sshJUtil;

    @Inject
    private ClouderaManagerUtil clouderaManagerUtil;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
        createEnvironmentWithNetworkAndFreeIpa(testContext);
        createDatalake(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running Manowar SDX cluster in available state",
            when = "Set a new workload user password for the acting user",
            and = "SSH to the MASTER node where checking the Cloudera Manager UUID file," +
                    " then checking CMAdmin group and HIVE resource config mappings via Cloudera Manager API",
            then = "SSH and Cloudera Manager access should be successful with new workload password, 'uuid' file" +
                    " and expected mappings should be present"
    )
    public void testSdxSshAndCmAccessWithNewWorkloadPassword(TestContext testContext) {
        String filePath = "/var/lib/cloudera-scm-agent";
        String fileName = "uuid";
        String newWorkloadPassword = "Admin@123";
        String workloadUsername = testContext.given(UmsTestDto.class).assignTarget(EnvironmentTestDto.class.getSimpleName())
                .when(umsTestClient.getUserDetails()).getResponse().getWorkloadUsername();

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
                .given(SdxInternalTestDto.class)
                .then((tc, testDto, client) -> sshJUtil.checkFilesOnHostByNameAndPath(testDto, getInstanceGroups(testDto, client), List.of(MASTER.getName()),
                            filePath, fileName, 1, workloadUsername, newWorkloadPassword))
                .then((tc, testDto, client) -> clouderaManagerUtil.checkClouderaManagerKnoxIDBrokerRoleConfigGroups(testDto, workloadUsername,
                        newWorkloadPassword))
                .validate();
    }
}
