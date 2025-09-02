package com.sequenceiq.it.cloudbreak.testcase.e2e.l0promotion;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.expectedMessage;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.auth.altus.UmsVirtualGroupRight;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.it.cloudbreak.assertion.ums.VirtualGroupTestAssertion;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.client.UmsTestClient;
import com.sequenceiq.it.cloudbreak.config.user.TestUserSelectors;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;
import com.sequenceiq.it.cloudbreak.dto.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.EnvironmentUtil;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshSudoCommandActions;

public class EnvironmentPrivilegedUserTest extends AbstractE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentPrivilegedUserTest.class);

    private static final String CHECK_IF_ROOT_COMMAND = "whoami | grep \"root\"";

    private static final String CHANGE_USER_TO_ROOT_COMMAND = "su";

    private static final String SSHD_RESTART = "systemctl restart sshd.service";

    private static final String SSSD_RESTART = "systemctl restart sssd.service";

    private static final String SSSD_STATUS = "systemctl status sssd.service | cat";

    private static final String BASH_SCRIPT = "#!/bin/bash";

    private static final String REPLACE_CLIENT_ALIVE_INTERVAL = "sed -i \"s/ClientAliveInterval.*/ClientAliveInterval 1800/g\" /etc/ssh/sshd_config";

    private static final String SSHD_CONFIG_RECIPE_CONTENT = Base64.encodeBase64String(String.join("\n",
            BASH_SCRIPT,
            REPLACE_CLIENT_ALIVE_INTERVAL,
            SSHD_RESTART
    ).getBytes());

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private UmsTestClient umsTestClient;

    @Inject
    private SshSudoCommandActions sshSudoCommandActions;

    @Inject
    private EnvironmentUtil environmentUtil;

    @Inject
    private RecipeTestClient recipeTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getTestUsers().setSelector(TestUserSelectors.UMS_ONLY);
        assertSupportedCloudPlatform(CloudPlatform.AWS);
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        testContext.as(L0UserKeys.USER_ACCOUNT_ADMIN);

        initializeDefaultBlueprints(testContext);
        testContext.as(L0UserKeys.ENV_CREATOR_A);
        createDefaultCredential(testContext);
        createRecipeAndApplyOnFreeIpaAndMasterNodes(testContext);
        createEnvironmentWithFreeIpa(testContext);
        createDatalakeWithoutDatabase(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is an up and running SDX cluster ",
            when = "the current user tries to run sudo commands on any VM in the cluster ",
            then = "execution should fail in case of EnvironmentPrivilegedUser role is not assigned ",
                and = "execution should pass in case of assigned EnvironmentPrivilegedUser role but changing the user to root should fail")
    public void testSudoCommands(TestContext testContext) {
        AtomicReference<Map<UmsVirtualGroupRight, String>> environmentVirtualGroups = new AtomicReference<>();

        testContext
                .given(UmsTestDto.class)
                    .assignTarget(EnvironmentTestDto.class.getSimpleName())
                    .withEnvironmentAdmin()
                .when(umsTestClient.assignResourceRole(L0UserKeys.USER_ENV_CREATOR))
                .validate();

        String workloadUsernameEnvCreator = testContext.getTestUsers().getUserByLabel(L0UserKeys.USER_ENV_CREATOR).getWorkloadUserName();
        testContext.as(L0UserKeys.USER_ENV_CREATOR);

        setWorkloadPassword(testContext);
        testContext
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .thenException((tc, testDto, client) -> {
                    sshSudoCommandActions.executeCommand(getIpAddresses(tc), workloadUsernameEnvCreator, tc.getWorkloadPassword(), CHECK_IF_ROOT_COMMAND);
                    return testDto;
                }, TestFailException.class, expectedMessage("sudo command failed on '.*' for user '" + workloadUsernameEnvCreator + "'."))
                .given(UmsTestDto.class)
                    .withEnvironmentPrivilegedUser()
                .when(umsTestClient.assignResourceRole(L0UserKeys.USER_ENV_CREATOR))
                .then((tc, dto, client) -> {
                    environmentVirtualGroups.set(environmentUtil.getEnvironmentVirtualGroups(tc, client));
                    return dto;
                })
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.syncAll())
                .await(OperationState.COMPLETED)
                .then((tc, testDto, client) -> {
                    Set<String> ipAddresses = getIpAddresses(tc);
                    sshSudoCommandActions.executeCommand(ipAddresses, null, null, SSSD_RESTART, SSSD_STATUS);
                    tc.waitingFor(Duration.ofMinutes(2), "Waiting for SSSD to be synchronized has been interrupted");
                    sshSudoCommandActions.executeCommand(ipAddresses, workloadUsernameEnvCreator, tc.getWorkloadPassword(), CHECK_IF_ROOT_COMMAND);
                    return testDto;
                })
                .validate();

        testContext
                .as(L0UserKeys.USER_ENV_CREATOR)
                .given(FreeIpaTestDto.class)
                .thenException((tc, testDto, client) -> {
                    sshSudoCommandActions.executeCommand(getIpAddresses(tc), workloadUsernameEnvCreator, tc.getWorkloadPassword(),
                            CHANGE_USER_TO_ROOT_COMMAND);
                    return testDto;
                }, TestFailException.class, expectedMessage("sudo command failed on '.*' for user '" + workloadUsernameEnvCreator + "'."))
                .validate();

        testContext
                .as(L0UserKeys.ENV_CREATOR_A)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.findUsers(Set.of(workloadUsernameEnvCreator), true))
                .then(VirtualGroupTestAssertion.validateAdminVirtualGroupMembership(freeIpaTestClient, environmentVirtualGroups.get(),
                        Set.of(workloadUsernameEnvCreator), true))
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

    private void createRecipeAndApplyOnFreeIpaAndMasterNodes(TestContext context) {
        String recipeName = resourcePropertyProvider().getName();
        context
                .given(recipeName, RecipeTestDto.class)
                    .withName(recipeName)
                    .withContent(SSHD_CONFIG_RECIPE_CONTENT)
                    .withRecipeType(RecipeV4Type.PRE_SERVICE_DEPLOYMENT)
                .when(recipeTestClient.createV4(), RunningParameter.key(recipeName));
        context.given("master", InstanceGroupTestDto.class).withRecipes(recipeName);
        context.given(EnvironmentTestDto.class).withFreeIpaRecipe(Collections.singleton(recipeName));
    }
}
