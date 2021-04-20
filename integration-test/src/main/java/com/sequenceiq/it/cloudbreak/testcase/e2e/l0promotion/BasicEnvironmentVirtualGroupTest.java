package com.sequenceiq.it.cloudbreak.testcase.e2e.l0promotion;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.auth.altus.UmsRight;
import com.sequenceiq.cloudbreak.logger.MDCUtils;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.it.cloudbreak.UmsClient;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakActor;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.UmsTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.testcase.authorization.AuthUserKeys;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class BasicEnvironmentVirtualGroupTest extends AbstractE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasicEnvironmentVirtualGroupTest.class);

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private UmsTestClient umsTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CloudbreakActor cloudbreakActor;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        useRealUmsUser(testContext, AuthUserKeys.USER_ACCOUNT_ADMIN);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
        createEnvironmentWithNetworkAndFreeIpa(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running Manowar Environment with synced FreeIPA",
            when = "assign then unassigned Environment Admin and Environment User",
            then = "FreeIPA should be successfully synced with new users and theirs resource roles.")
    public void testAddUsersToEnvironment(TestContext testContext) {
        AtomicReference<Map<UmsRight, String>> environmentVirtualGroups = new AtomicReference<>();
        String workloadUsernameEnvAdminA = testContext.getRealUmsUserByKey(AuthUserKeys.ENV_ADMIN_A).getWorkloadUserName();
        String workloadUsernameEnvCreatorA =  testContext.getRealUmsUserByKey(AuthUserKeys.ENV_CREATOR_A).getWorkloadUserName();

        testContext
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.getLastSyncOperationStatus())
                .await(OperationState.COMPLETED)
                .given(UmsTestDto.class)
                .assignTarget(EnvironmentTestDto.class.getSimpleName())
                .withEnvironmentAdmin()
                .when(umsTestClient.assignResourceRole(AuthUserKeys.ENV_ADMIN_A))
                .withEnvironmentUser()
                .when(umsTestClient.assignResourceRole(AuthUserKeys.ENV_CREATOR_A))
                .then((context, dto, client) -> {
                    environmentVirtualGroups.set(getEnvironmentVirtualGroups(context, client));
                    return dto;
                })
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.syncAll())
                .await(OperationState.COMPLETED)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.findUsers(Set.of(workloadUsernameEnvAdminA, workloadUsernameEnvCreatorA), true))
                .then((context, dto, client) -> validateNewEnvironmentUserGroupMembership(dto, environmentVirtualGroups.get(),
                        Set.of(workloadUsernameEnvCreatorA), true))
                .validate();
        testContext
                .given(UmsTestDto.class)
                .assignTarget(EnvironmentTestDto.class.getSimpleName())
                .withEnvironmentAdmin()
                .when(umsTestClient.assignResourceRole(AuthUserKeys.ENV_CREATOR_A))
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.syncAll())
                .await(OperationState.COMPLETED)
                .given(FreeIpaTestDto.class)
                .then((context, dto, client) -> validateNewEnvironmentAdminGroupMembership(dto, environmentVirtualGroups.get(),
                        Set.of(workloadUsernameEnvAdminA, workloadUsernameEnvCreatorA), true))
                .validate();
        testContext
                .given(UmsTestDto.class)
                .assignTarget(EnvironmentTestDto.class.getSimpleName())
                .withEnvironmentAdmin()
                .when(umsTestClient.unAssignResourceRole(AuthUserKeys.ENV_ADMIN_A))
                .withEnvironmentUser()
                .when(umsTestClient.unAssignResourceRole(AuthUserKeys.ENV_CREATOR_A))
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.syncAll())
                .await(OperationState.COMPLETED)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.findUsers(Set.of(workloadUsernameEnvAdminA, workloadUsernameEnvCreatorA), false))
                .then((context, dto, client) -> validateNewEnvironmentAdminGroupMembership(dto, environmentVirtualGroups.get(),
                        Set.of(workloadUsernameEnvCreatorA), false))
                .validate();
    }

    private Map<UmsRight, String> getEnvironmentVirtualGroups(TestContext testContext, UmsClient client) {
        String accountId = testContext.getActingUserCrn().getAccountId();
        String environmentCrn = testContext.given(EnvironmentTestDto.class).getCrn();
        Map<UmsRight, String> virtualGroups = new HashMap<>();
        String virtualGroup = null;

        for (UmsRight right : UmsRight.values()) {
            try {
                virtualGroup = client.getDefaultClient().getWorkloadAdministrationGroupName(INTERNAL_ACTOR_CRN, accountId, MDCUtils.getRequestId(),
                        right.getRight(), environmentCrn);
            } catch (StatusRuntimeException ex) {
                if (Status.Code.NOT_FOUND != ex.getStatus().getCode()) {
                    LOGGER.info(String.format(" Virtual groups is missing for right: '%s' ", right.getRight()));
                }
            }
            if (StringUtils.hasText(virtualGroup)) {
                virtualGroups.put(right, virtualGroup);
            }
        }

        if (MapUtils.isNotEmpty(virtualGroups)) {
            LOGGER.info(String.format(" Virtual groups are present [%s] for environment '%s' ", virtualGroups, environmentCrn));
        } else {
            throw new TestFailException(String.format(" Cannot find virtual groups for environment '%s' ", environmentCrn));
        }
        return virtualGroups;
    }

    private FreeIpaTestDto validateNewEnvironmentAdminGroupMembership(FreeIpaTestDto testDto, Map<UmsRight, String> environmentVirtualGroups,
            Set<String> adminUsers, boolean expectedPresence) {
        List<String> adminGroups = environmentVirtualGroups
                .entrySet().stream()
                .filter(group -> group.getKey().getRight().contains("ADMIN"))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        LOGGER.info(String.format(" Admin groups are present [%s] at environment '%s' ", adminGroups, testDto.getResponse().getEnvironmentCrn()));
        adminGroups.forEach(adminGroup -> freeIpaTestClient.findUsersInGroup(adminUsers, adminGroup, expectedPresence));
        return testDto;
    }

    private FreeIpaTestDto validateNewEnvironmentUserGroupMembership(FreeIpaTestDto testDto, Map<UmsRight, String> environmentVirtualGroups,
            Set<String> environmentUsers, boolean expectedPresence) {
        List<String> userGroups = environmentVirtualGroups
                .entrySet().stream()
                .filter(group -> !group.getKey().getRight().contains("ADMIN"))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        LOGGER.info(String.format(" User groups are present [%s] at environment '%s' ", userGroups, testDto.getResponse().getEnvironmentCrn()));
        userGroups.forEach(adminGroup -> freeIpaTestClient.findUsersInGroup(environmentUsers, adminGroup, expectedPresence));
        return testDto;
    }
}
