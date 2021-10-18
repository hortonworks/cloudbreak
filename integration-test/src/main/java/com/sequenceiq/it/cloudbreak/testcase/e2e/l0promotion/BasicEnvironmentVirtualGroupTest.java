package com.sequenceiq.it.cloudbreak.testcase.e2e.l0promotion;

import static com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto.getIamGroupAdminCrn;
import static java.lang.String.format;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.testng.annotations.Test;

import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.auth.altus.UmsRight;
import com.sequenceiq.cloudbreak.logger.MDCUtils;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.it.cloudbreak.UmsClient;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.UmsTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class BasicEnvironmentVirtualGroupTest extends AbstractE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasicEnvironmentVirtualGroupTest.class);

    @Value("${integrationtest.aws.l0.adminGroupName}")
    private String adminGroupName;

    @Value("${integrationtest.aws.l0.adminGroupCrn}")
    private String adminGroupCrn;

    @Value("${integrationtest.aws.l0.userGroupName}")
    private String userGroupName;

    @Value("${integrationtest.aws.l0.userGroupCrn}")
    private String userGroupCrn;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private UmsTestClient umsTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        useRealUmsUser(testContext, L0UserKeys.USER_ACCOUNT_ADMIN);
        initializeDefaultBlueprints(testContext);
        useRealUmsUser(testContext, L0UserKeys.ENV_CREATOR_A);
        createDefaultCredential(testContext);
        createEnvironmentWithFreeIpa(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running Manowar Environment with synced FreeIPA",
            when = "assign then unassigned Environment Admin and Environment User",
            then = "FreeIPA should be successfully synced with new users and theirs resource roles.")
    public void testAddUsersToEnvironment(TestContext testContext) {
        AtomicReference<Map<UmsRight, String>> environmentVirtualGroups = new AtomicReference<>();
        String workloadUsernameEnvAdminA = testContext.getRealUmsUserByKey(L0UserKeys.ENV_ADMIN_A).getWorkloadUserName();
        String workloadUsernameEnvCreatorB =  testContext.getRealUmsUserByKey(L0UserKeys.ENV_CREATOR_B).getWorkloadUserName();

        useRealUmsUser(testContext, L0UserKeys.ENV_CREATOR_A);

        testContext
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.getLastSyncOperationStatus())
                .await(OperationState.COMPLETED)
                .given(UmsTestDto.class)
                .assignTarget(EnvironmentTestDto.class.getSimpleName())
                .withEnvironmentAdmin()
                .when(umsTestClient.assignResourceRole(L0UserKeys.ENV_ADMIN_A))
                .withEnvironmentUser()
                .when(umsTestClient.assignResourceRole(L0UserKeys.ENV_CREATOR_B))
                .then((tc, dto, client) -> {
                    environmentVirtualGroups.set(getEnvironmentVirtualGroups(tc, client));
                    return dto;
                })
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.syncAll())
                .await(OperationState.COMPLETED)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.findUsers(Set.of(workloadUsernameEnvAdminA, workloadUsernameEnvCreatorB), true))
                .then((tc, dto, client) -> validateUserVirtualGroupMembership(tc, dto, environmentVirtualGroups.get(),
                        Set.of(workloadUsernameEnvCreatorB), true))
                .validate();

        testContext
                .given(UmsTestDto.class)
                .assignTarget(EnvironmentTestDto.class.getSimpleName())
                .withEnvironmentAdmin()
                .when(umsTestClient.assignResourceRole(L0UserKeys.ENV_CREATOR_B))
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.syncAll())
                .await(OperationState.COMPLETED)
                .given(FreeIpaTestDto.class)
                .then((tc, dto, client) -> validateAdminVirtualGroupMembership(tc, dto, environmentVirtualGroups.get(),
                        Set.of(workloadUsernameEnvAdminA, workloadUsernameEnvCreatorB), true))
                .validate();

        testContext
                .given(UmsTestDto.class)
                .assignTarget(EnvironmentTestDto.class.getSimpleName())
                .withEnvironmentAdmin()
                .when(umsTestClient.unAssignResourceRole(L0UserKeys.ENV_ADMIN_A))
                .withEnvironmentUser()
                .when(umsTestClient.unAssignResourceRole(L0UserKeys.ENV_CREATOR_B))
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.syncAll())
                .await(OperationState.COMPLETED)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.findUsers(Set.of(workloadUsernameEnvAdminA, workloadUsernameEnvCreatorB), false))
                .then((tc, dto, client) -> validateAdminVirtualGroupMembership(tc, dto, environmentVirtualGroups.get(),
                        Set.of(workloadUsernameEnvCreatorB), false))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running Manowar Environment with synced FreeIPA",
            when = "add then remove admin and user groups to Environment",
            then = "FreeIPA should be successfully synced with new groups and theirs resource roles.")
    public void testAddGroupsToEnvironment(TestContext testContext) {
        AtomicReference<Map<UmsRight, String>> environmentVirtualGroups = new AtomicReference<>();
        CloudbreakUser userEnvAdminA = testContext.getRealUmsUserByKey(L0UserKeys.ENV_ADMIN_A);
        CloudbreakUser userEnvCreatorB =  testContext.getRealUmsUserByKey(L0UserKeys.ENV_CREATOR_B);
        CloudbreakUser userEnvCreatorA =  testContext.getRealUmsUserByKey(L0UserKeys.ENV_CREATOR_A);

        useRealUmsUser(testContext, L0UserKeys.ENV_CREATOR_A);

        testContext
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.getLastSyncOperationStatus())
                .await(OperationState.COMPLETED)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .validate();

        testContext
                .given(UmsTestDto.class)
                .assignTargetByCrn(adminGroupCrn)
                .withGroupAdmin()
                .when(umsTestClient.assignResourceRole(L0UserKeys.ENV_CREATOR_A))
                .then((tc, dto, client) -> validateAssignedResourceRole(tc, dto, client, userEnvCreatorA, getIamGroupAdminCrn(), true))
                .assignTargetByCrn(userGroupCrn)
                .withGroupAdmin()
                .when(umsTestClient.assignResourceRole(L0UserKeys.ENV_CREATOR_A))
                .then((tc, dto, client) -> validateAssignedResourceRole(tc, dto, client, userEnvCreatorA, getIamGroupAdminCrn(), true))
                .given(UmsGroupTestDto.class)
                .when(umsTestClient.addUserToGroup(adminGroupName, userEnvAdminA.getCrn()))
                .when(umsTestClient.addUserToGroup(userGroupName, userEnvCreatorB.getCrn()))
                .then((tc, dto, client) -> validateUserGroupMembership(tc, dto, client, userEnvAdminA, adminGroupName, true))
                .then((tc, dto, client) -> validateUserGroupMembership(tc, dto, client, userEnvCreatorB, userGroupName, true))
                .validate();

        testContext
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.getLastSyncOperationStatus())
                .await(OperationState.COMPLETED)
                .given(UmsTestDto.class)
                .assignTarget(EnvironmentTestDto.class.getSimpleName())
                .withEnvironmentAdmin()
                .when(umsTestClient.assignResourceRoleWithGroup(adminGroupCrn))
                .withEnvironmentUser()
                .when(umsTestClient.assignResourceRoleWithGroup(userGroupCrn))
                .then((tc, dto, client) -> {
                    environmentVirtualGroups.set(getEnvironmentVirtualGroups(tc, client));
                    return dto;
                })
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.syncAll())
                .await(OperationState.COMPLETED)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.findGroups(Set.of(adminGroupName, userGroupName)))
                .then((tc, dto, client) -> validateAdminVirtualGroupMembership(tc, dto, environmentVirtualGroups.get(),
                        Set.of(userEnvAdminA.getWorkloadUserName()), true))
                .then((tc, dto, client) -> validateUserVirtualGroupMembership(tc, dto, environmentVirtualGroups.get(),
                        Set.of(userEnvCreatorB.getWorkloadUserName()), true))
                .validate();

        testContext
                .given(UmsGroupTestDto.class)
                .when(umsTestClient.removeUserFromGroup(adminGroupName, userEnvAdminA.getCrn()))
                .when(umsTestClient.removeUserFromGroup(userGroupName, userEnvCreatorB.getCrn()))
                .then((tc, dto, client) -> validateUserGroupMembership(tc, dto, client, userEnvAdminA, adminGroupName, false))
                .then((tc, dto, client) -> validateUserGroupMembership(tc, dto, client, userEnvCreatorB, userGroupName, false))
                .validate();

        testContext
                .given(UmsTestDto.class)
                .assignTargetByCrn(adminGroupCrn)
                .withGroupAdmin()
                .when(umsTestClient.unAssignResourceRole(L0UserKeys.ENV_CREATOR_A))
                .then((tc, dto, client) -> validateAssignedResourceRole(tc, dto, client, userEnvCreatorA, getIamGroupAdminCrn(), false))
                .assignTargetByCrn(userGroupCrn)
                .withGroupAdmin()
                .when(umsTestClient.unAssignResourceRole(L0UserKeys.ENV_CREATOR_A))
                .then((tc, dto, client) -> validateAssignedResourceRole(tc, dto, client, userEnvCreatorA, getIamGroupAdminCrn(), false))
                .validate();
    }

    private Map<UmsRight, String> getEnvironmentVirtualGroups(TestContext testContext, UmsClient client) {
        String accountId = testContext.getActingUserCrn().getAccountId();
        String environmentCrn = testContext.given(EnvironmentTestDto.class).getCrn();
        Map<UmsRight, String> virtualGroups = new HashMap<>();
        String virtualGroup = null;

        for (UmsRight right : UmsRight.values()) {
            try {
                virtualGroup = client.getDefaultClient().getWorkloadAdministrationGroupName(accountId, MDCUtils.getRequestId(),
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
            Log.then(LOGGER, format(" Virtual groups are present [%s] for environment '%s' ", virtualGroups, environmentCrn));
            LOGGER.info(String.format(" Virtual groups are present [%s] for environment '%s' ", virtualGroups, environmentCrn));
        } else {
            throw new TestFailException(String.format(" Cannot find virtual groups for environment '%s' ", environmentCrn));
        }

        return virtualGroups;
    }

    private FreeIpaTestDto validateAdminVirtualGroupMembership(TestContext testContext, FreeIpaTestDto testDto, Map<UmsRight, String> environmentVirtualGroups,
            Set<String> adminUsers, boolean expectedPresence) {

        List<String> adminGroups = environmentVirtualGroups
                .entrySet().stream()
                .filter(group -> group.getKey().name().contains("ADMIN"))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        LOGGER.info(String.format(" Admin groups are present [%s] at environment '%s' ", adminGroups, testDto.getResponse().getEnvironmentCrn()));
        adminGroups.forEach(adminGroup -> freeIpaTestClient.findUsersInGroup(adminUsers, adminGroup, expectedPresence));

        return testDto;
    }

    private FreeIpaTestDto validateUserVirtualGroupMembership(TestContext testContext, FreeIpaTestDto testDto, Map<UmsRight, String> environmentVirtualGroups,
            Set<String> environmentUsers, boolean expectedPresence) {

        List<String> userGroups = environmentVirtualGroups
                .entrySet().stream()
                .filter(group -> group.getKey().name().contains("ACCESS"))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        LOGGER.info(String.format(" User groups are present [%s] at environment '%s' ", userGroups, testDto.getResponse().getEnvironmentCrn()));
        userGroups.forEach(userGroup -> freeIpaTestClient.findUsersInGroup(environmentUsers, userGroup, expectedPresence));

        return testDto;
    }

    private UmsGroupTestDto validateUserGroupMembership(TestContext testContext, UmsGroupTestDto umsGroupTestDto, UmsClient client, CloudbreakUser groupMember,
            String groupName, boolean expectedPresence) {
        String accountId = testContext.getActingUserCrn().getAccountId();

        List<String> groupMembers = client.getDefaultClient().listMembersFromGroup(accountId, groupName,
                Optional.of(""));
        boolean memberPresent = groupMembers.stream().anyMatch(memberCrn -> groupMember.getCrn().equals(memberCrn));
        LOGGER.info("Member is present '{}' at group '{}', group members: [{}]", memberPresent, groupName, groupMembers);
        if (expectedPresence) {
            if (memberPresent) {
                LOGGER.info("User '{}' have been assigned successfully to group {}.", groupMember.getDisplayName(), groupName);
                Log.then(LOGGER, format(" User '%s' have been assigned successfully to group '%s'. ", groupMember.getDisplayName(), groupName));
            } else {
                throw new TestFailException(String.format(" User '%s' is missing from group '%s' members! ", groupMember.getDisplayName(), groupName));
            }
        } else {
            if (!memberPresent) {
                LOGGER.info("User '{}' have been removed successfully from group {}.", groupMember.getDisplayName(), groupName);
                Log.then(LOGGER, format(" User '%s' have been removed successfully from group '%s'. ", groupMember.getDisplayName(), groupName));
            } else {
                throw new TestFailException(String.format(" User '%s' is still member of group '%s'! ", groupMember.getDisplayName(), groupName));
            }
        }
        return umsGroupTestDto;
    }

    private UmsTestDto validateAssignedResourceRole(TestContext testContext, UmsTestDto umsTestDto, UmsClient client, CloudbreakUser assignee,
            String roleCrn, boolean expectedPresence) {
        String resourceCrn = umsTestDto.getRequest().getResourceCrn();
        String userCrn = assignee.getCrn();

        LOGGER.info(format(" Validate resource role '%s' has been successfully assigned to user '%s' at resource '%s'... ", roleCrn, userCrn, resourceCrn));
        Multimap<String, String> assignedResourceRoles = client.getDefaultClient().listAssignedResourceRoles(userCrn, Optional.of(""));
        boolean resourceRoleAssigned = assignedResourceRoles.get(resourceCrn).contains(roleCrn);
        if (expectedPresence) {
            if (resourceRoleAssigned) {
                LOGGER.info(format(" Resource role '%s' has successfully been assigned to user '%s' at resource '%s' ", roleCrn, userCrn,
                        resourceCrn));
                Log.then(LOGGER, format(" Resource role '%s' has successfully been assigned to user '%s' at resource '%s' ", roleCrn, userCrn,
                        resourceCrn));
            } else {
                throw new TestFailException(String.format(" Resource role '%s' has not been assigned to user '%s' at resource '%s'! ", roleCrn,
                        userCrn, resourceCrn));
            }
        } else {
            if (!resourceRoleAssigned) {
                LOGGER.info(format(" Resource role '%s' has successfully been revoked from user '%s' at resource '%s' ", roleCrn, userCrn,
                        resourceCrn));
                Log.then(LOGGER, format(" Resource role '%s' has successfully been revoked from user '%s' at resource '%s' ", roleCrn,
                        userCrn, resourceCrn));
            } else {
                throw new TestFailException(String.format(" Resource role '%s' has not been revoked from user '%s' at resource '%s'! ", roleCrn,
                        userCrn, resourceCrn));
            }
        }
        return umsTestDto;
    }
}
