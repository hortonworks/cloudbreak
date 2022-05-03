package com.sequenceiq.it.cloudbreak.testcase.e2e.l0promotion;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.expectedMessage;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
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
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshSudoCommandActions;

public class EnvironmentPrivilegedUserTest extends AbstractE2ETest {

    private static final String LIST_RULES_FLAG = "-l";

    private static final String CHANGE_USER_TO_ROOT_COMMAND = "su";

    private static final String SSSD_RESTART = "systemctl restart sssd.service";

    private static final String SSSD_STATUS = "systemctl status sssd.service";

    @Value("${integrationtest.user.workloadPassword:}")
    private String workloadPassword;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private UmsTestClient umsTestClient;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private SshSudoCommandActions sshSudoCommandActions;

    @Override
    protected void setupTest(TestContext testContext) {
        assertSupportedCloudPlatform(CloudPlatform.AWS);
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        useRealUmsUser(testContext, L0UserKeys.USER_ACCOUNT_ADMIN);
        initializeDefaultBlueprints(testContext);

        useRealUmsUser(testContext, L0UserKeys.ENV_CREATOR_A);
        createDefaultCredential(testContext);
        createDefaultDatalake(testContext);
    }

    @Override
    public void tearDownSpotValidateTags(Object[] data) {
        useRealUmsUser((TestContext) data[0], L0UserKeys.ENV_CREATOR_A);
        super.tearDownSpotValidateTags(data);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is an up and running SDX cluster ",
            when = "the current user tries to run sudo commands on any VM in the cluster ",
            then = "execution should fail in case of EnvironmentPrivilegedUser role is not assigned ",
                and = "execution should pass in case of assigned EnvironmentPrivilegedUser role but changing the user to root should fail"
    )
    public void testSudoCommands(TestContext testContext) {
        testContext
                .given(UmsTestDto.class)
                .assignTarget(EnvironmentTestDto.class.getSimpleName())
                .withEnvironmentAdmin()
                .when(umsTestClient.assignResourceRole(L0UserKeys.USER_ENV_CREATOR, regionAwareInternalCrnGeneratorFactory))
                .validate();

        String workloadUsernameEnvCreator = testContext.getRealUmsUserByKey(L0UserKeys.USER_ENV_CREATOR).getWorkloadUserName();
        useRealUmsUser(testContext, L0UserKeys.USER_ENV_CREATOR);

        testContext
                .given(UmsTestDto.class).assignTarget(EnvironmentTestDto.class.getSimpleName())
                .when(umsTestClient.setWorkloadPassword(workloadPassword, regionAwareInternalCrnGeneratorFactory))
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.syncAll())
                .await(OperationState.COMPLETED)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .thenException((tc, testDto, client) -> {
                    sshSudoCommandActions.executeCommand(getIpAddresses(tc), workloadUsernameEnvCreator, workloadPassword, LIST_RULES_FLAG);
                    return testDto;
                }, TestFailException.class, expectedMessage("sudo command failed on '.*' for user '" + workloadUsernameEnvCreator + "'."))
                .given(UmsTestDto.class)
                .withEnvironmentPrivilegedUser()
                .when(umsTestClient.assignResourceRole(L0UserKeys.USER_ENV_CREATOR, regionAwareInternalCrnGeneratorFactory))
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.syncAll())
                .await(OperationState.COMPLETED)
                .then((tc, testDto, client) -> {
                    Set<String> ipAddresses = getIpAddresses(tc);
                    sshSudoCommandActions.executeCommand(ipAddresses, null, null, SSSD_RESTART, SSSD_STATUS);
                    tc.waitingFor(Duration.ofMinutes(2), "Waiting for SSSD to be synchronized has been interrupted");
                    sshSudoCommandActions.executeCommand(ipAddresses, workloadUsernameEnvCreator, workloadPassword, LIST_RULES_FLAG);
                    return testDto;
                })
                .thenException((tc, testDto, client) -> {
                    sshSudoCommandActions.executeCommand(getIpAddresses(tc), workloadUsernameEnvCreator, workloadPassword, CHANGE_USER_TO_ROOT_COMMAND);
                    return testDto;
                }, TestFailException.class, expectedMessage("sudo command failed on '.*' for user '" + workloadUsernameEnvCreator + "'."))
                .validate();
    }

    private Set<String> getIpAddresses(TestContext testContext) {
        Set<String> ipAddresses = testContext.get(FreeIpaTestDto.class).getResponse().getFreeIpa().getServerIp();
        ipAddresses.addAll(getSdxInternalStackPrivateIpAddressesExcludingIdBroker(testContext));

        return ipAddresses;
    }

    private Set<String> getSdxInternalStackPrivateIpAddressesExcludingIdBroker(TestContext context) {
        SdxInternalTestDto dto = context.get(SdxInternalTestDto.class);
        return dto.getResponse().getStackV4Response().getInstanceGroups().stream()
                .flatMap(instanceGroupV4Response -> instanceGroupV4Response.getMetadata().stream())
                .filter(instanceMetaDataV4Response -> !"idbroker".equalsIgnoreCase(instanceMetaDataV4Response.getInstanceGroup()))
                .map(InstanceMetaDataV4Response::getPrivateIp)
                .collect(Collectors.toSet());
    }
}
