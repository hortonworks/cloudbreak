package com.sequenceiq.it.cloudbreak.testcase.e2e.l0promotion;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;

import java.util.List;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.it.cloudbreak.assertion.datalake.RecipeTestAssertion;
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

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Value("${integrationtest.user.workloadPassword:}")
    private String workloadPassword;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
        createDefaultDatalake(testContext);
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
        String userCrn = testContext.getActingUserCrn().toString();
        String workloadUsername = testContext.given(UmsTestDto.class).assignTarget(EnvironmentTestDto.class.getSimpleName())
                .when(umsTestClient.getUserDetails(userCrn, regionAwareInternalCrnGeneratorFactory)).getResponse().getWorkloadUsername();

        testContext
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.getLastSyncOperationStatus())
                .await(OperationState.COMPLETED)
                .given(UmsTestDto.class).assignTarget(EnvironmentTestDto.class.getSimpleName())
                .when(umsTestClient.setWorkloadPassword(workloadPassword, regionAwareInternalCrnGeneratorFactory))
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.syncAll())
                .await(OperationState.COMPLETED)
                .given(SdxInternalTestDto.class)
                .then(RecipeTestAssertion.validateFilesOnHost(List.of(MASTER.getName()), filePath, fileName, 1, workloadUsername, workloadPassword,
                        sshJUtil))
                .then((tc, testDto, client) -> clouderaManagerUtil.checkClouderaManagerKnoxIDBrokerRoleConfigGroups(testDto, workloadUsername,
                        workloadPassword))
                .validate();
    }
}
