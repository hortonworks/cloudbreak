package com.sequenceiq.it.cloudbreak.testcase.e2e.l0promotion;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.auth.altus.UmsVirtualGroupRight;
import com.sequenceiq.cloudbreak.auth.altus.service.UmsResourceRole;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.assertion.ums.ResourceRoleTestAssertion;
import com.sequenceiq.it.cloudbreak.assertion.ums.UserGroupTestAssertion;
import com.sequenceiq.it.cloudbreak.assertion.ums.VirtualGroupTestAssertion;
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
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.EnvironmentUtil;
import com.sequenceiq.it.util.imagevalidation.ImageValidatorE2ETest;

public class BasicEnvironmentVirtualGroupTest extends AbstractE2ETest implements ImageValidatorE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasicEnvironmentVirtualGroupTest.class);

    @Value("${integrationtest.userGroup.adminGroupName:}")
    private String adminGroupName;

    @Value("${integrationtest.userGroup.adminGroupCrn:}")
    private String adminGroupCrn;

    @Value("${integrationtest.userGroup.userGroupName:}")
    private String userGroupName;

    @Value("${integrationtest.userGroup.userGroupCrn:}")
    private String userGroupCrn;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private UmsTestClient umsTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private EnvironmentUtil environmentUtil;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        useRealUmsUser(testContext, L0UserKeys.USER_ACCOUNT_ADMIN);
        initializeDefaultBlueprints(testContext);
        useRealUmsUser(testContext, L0UserKeys.ENV_CREATOR_A);
        createDefaultCredential(testContext);
        createDefaultEnvironment(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running Manowar Environment with synced FreeIPA",
            when = "assign then unassigned Environment Admin and Environment User",
            then = "FreeIPA should be successfully synced with new users and theirs resource roles.")
    public void testAddUsersToEnvironment(TestContext testContext) {
        AtomicReference<Map<UmsVirtualGroupRight, String>> environmentVirtualGroups = new AtomicReference<>();
        String workloadUsernameEnvAdminA = testContext.getTestUsers().getUserByLabel(L0UserKeys.ENV_ADMIN_A).getWorkloadUserName();
        String workloadUsernameEnvCreatorB = testContext.getTestUsers().getUserByLabel(L0UserKeys.ENV_CREATOR_B).getWorkloadUserName();

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
                    environmentVirtualGroups.set(environmentUtil.getEnvironmentVirtualGroups(tc, client));
                    return dto;
                })
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.syncAll())
                .await(OperationState.COMPLETED)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.findUsers(Set.of(workloadUsernameEnvAdminA, workloadUsernameEnvCreatorB), true))
                .then(VirtualGroupTestAssertion.validateUserVirtualGroupMembership(freeIpaTestClient, environmentVirtualGroups.get(),
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
                .then(VirtualGroupTestAssertion.validateAdminVirtualGroupMembership(freeIpaTestClient, environmentVirtualGroups.get(),
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
                .then(VirtualGroupTestAssertion.validateAdminVirtualGroupMembership(freeIpaTestClient, environmentVirtualGroups.get(),
                        Set.of(workloadUsernameEnvCreatorB), false))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running Manowar Environment with synced FreeIPA",
            when = "add then remove admin and user groups to Environment",
            then = "FreeIPA should be successfully synced with new groups and theirs resource roles.")
    public void testAddGroupsToEnvironment(TestContext testContext) {
        AtomicReference<Map<UmsVirtualGroupRight, String>> environmentVirtualGroups = new AtomicReference<>();
        CloudbreakUser userEnvAdminA = testContext.getTestUsers().getUserByLabel(L0UserKeys.ENV_ADMIN_A);
        CloudbreakUser userEnvCreatorB = testContext.getTestUsers().getUserByLabel(L0UserKeys.ENV_CREATOR_B);
        CloudbreakUser userEnvCreatorA = testContext.getTestUsers().getUserByLabel(L0UserKeys.ENV_CREATOR_A);

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
                .then(ResourceRoleTestAssertion.validateAssignedResourceRole(userEnvCreatorA, UmsResourceRole.IAM_GROUP_ADMIN, true))
                .assignTargetByCrn(userGroupCrn)
                .withGroupAdmin()
                .when(umsTestClient.assignResourceRole(L0UserKeys.ENV_CREATOR_A))
                .then(ResourceRoleTestAssertion.validateAssignedResourceRole(userEnvCreatorA, UmsResourceRole.IAM_GROUP_ADMIN, true))
                .given(UmsGroupTestDto.class)
                .when(umsTestClient.addUserToGroup(adminGroupName, userEnvAdminA.getCrn()))
                .when(umsTestClient.addUserToGroup(userGroupName, userEnvCreatorB.getCrn()))
                .then(UserGroupTestAssertion.validateUserGroupMembership(userEnvAdminA, adminGroupName, true))
                .then(UserGroupTestAssertion.validateUserGroupMembership(userEnvCreatorB, userGroupName, true))
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
                    environmentVirtualGroups.set(environmentUtil.getEnvironmentVirtualGroups(tc, client));
                    return dto;
                })
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.syncAll())
                .await(OperationState.COMPLETED)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.findGroups(Set.of(adminGroupName, userGroupName)))
                .then(VirtualGroupTestAssertion.validateAdminVirtualGroupMembership(freeIpaTestClient, environmentVirtualGroups.get(),
                        Set.of(userEnvAdminA.getWorkloadUserName()), true))
                .then(VirtualGroupTestAssertion.validateUserVirtualGroupMembership(freeIpaTestClient, environmentVirtualGroups.get(),
                        Set.of(userEnvCreatorB.getWorkloadUserName()), true))
                .validate();

        testContext
                .given(UmsGroupTestDto.class)
                .when(umsTestClient.removeUserFromGroup(adminGroupName, userEnvAdminA.getCrn()))
                .when(umsTestClient.removeUserFromGroup(userGroupName, userEnvCreatorB.getCrn()))
                .then(UserGroupTestAssertion.validateUserGroupMembership(userEnvAdminA, adminGroupName, false))
                .then(UserGroupTestAssertion.validateUserGroupMembership(userEnvCreatorB, userGroupName, false))
                .validate();

        testContext
                .given(UmsTestDto.class)
                .assignTargetByCrn(adminGroupCrn)
                .withGroupAdmin()
                .when(umsTestClient.unAssignResourceRole(L0UserKeys.ENV_CREATOR_A))
                .then(ResourceRoleTestAssertion.validateAssignedResourceRole(userEnvCreatorA, UmsResourceRole.IAM_GROUP_ADMIN, false))
                .assignTargetByCrn(userGroupCrn)
                .withGroupAdmin()
                .when(umsTestClient.unAssignResourceRole(L0UserKeys.ENV_CREATOR_A))
                .then(ResourceRoleTestAssertion.validateAssignedResourceRole(userEnvCreatorA, UmsResourceRole.IAM_GROUP_ADMIN, false))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running Manowar Environment with synced FreeIPA",
            when = "add two user groups with admin privileges and with common users to Environment",
                and = "remove admin privileges from one of the user groups",
            then = "all the common users should still have admin privileges.")
    public void testAddMultiGroupsUsersToEnvironment(TestContext testContext) {
        AtomicReference<Map<UmsVirtualGroupRight, String>> environmentVirtualGroups = new AtomicReference<>();
        CloudbreakUser userEnvAdminA = testContext.getTestUsers().getUserByLabel(L0UserKeys.ENV_ADMIN_A);
        CloudbreakUser userEnvCreatorB = testContext.getTestUsers().getUserByLabel(L0UserKeys.ENV_CREATOR_B);
        CloudbreakUser userEnvCreatorA = testContext.getTestUsers().getUserByLabel(L0UserKeys.ENV_CREATOR_A);
        String testMultiGroupAName = "testmultigroupa";
        String testMultiGroupBName = "testmultigroupb";
        AtomicReference<String> testMultiGroupACrn = new AtomicReference<>();
        AtomicReference<String> testMultiGroupBCrn = new AtomicReference<>();

        testContext
                .given(UmsGroupTestDto.class)
                .when(umsTestClient.createUserGroup(testMultiGroupAName))
                .then((tc, dto, client) -> {
                    testMultiGroupACrn.set(dto.getResponse().getCrn());
                    return dto;
                })
                .then(UserGroupTestAssertion.validateUserGroupPresence(testMultiGroupAName, true))
                .when(umsTestClient.createUserGroup(testMultiGroupBName))
                .then((tc, dto, client) -> {
                    testMultiGroupBCrn.set(dto.getResponse().getCrn());
                    return dto;
                })
                .then(UserGroupTestAssertion.validateUserGroupPresence(testMultiGroupBName, true))
                .validate();

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
                .assignTargetByCrn(testMultiGroupACrn.get())
                    .withGroupAdmin()
                .when(umsTestClient.assignResourceRole(L0UserKeys.ENV_CREATOR_A))
                .then(ResourceRoleTestAssertion.validateAssignedResourceRole(userEnvCreatorA, UmsResourceRole.IAM_GROUP_ADMIN, true))
                .assignTargetByCrn(testMultiGroupBCrn.get())
                    .withGroupAdmin()
                .when(umsTestClient.assignResourceRole(L0UserKeys.ENV_CREATOR_A))
                .then(ResourceRoleTestAssertion.validateAssignedResourceRole(userEnvCreatorA, UmsResourceRole.IAM_GROUP_ADMIN, true))
                .given(UmsGroupTestDto.class)
                .when(umsTestClient.addUserToGroup(testMultiGroupAName, userEnvAdminA.getCrn()))
                .when(umsTestClient.addUserToGroup(testMultiGroupAName, userEnvCreatorB.getCrn()))
                .when(umsTestClient.addUserToGroup(testMultiGroupBName, userEnvAdminA.getCrn()))
                .when(umsTestClient.addUserToGroup(testMultiGroupBName, userEnvCreatorB.getCrn()))
                .then(UserGroupTestAssertion.validateUserGroupMembership(userEnvAdminA, testMultiGroupAName, true))
                .then(UserGroupTestAssertion.validateUserGroupMembership(userEnvCreatorB, testMultiGroupAName, true))
                .then(UserGroupTestAssertion.validateUserGroupMembership(userEnvAdminA, testMultiGroupBName, true))
                .then(UserGroupTestAssertion.validateUserGroupMembership(userEnvCreatorB, testMultiGroupBName, true))
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
                .when(umsTestClient.assignResourceRoleWithGroup(testMultiGroupACrn.get()))
                    .withEnvironmentAdmin()
                .when(umsTestClient.assignResourceRoleWithGroup(testMultiGroupBCrn.get()))
                .then((tc, dto, client) -> {
                    environmentVirtualGroups.set(environmentUtil.getEnvironmentVirtualGroups(tc, client));
                    return dto;
                })
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.syncAll())
                .await(OperationState.COMPLETED)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.findGroups(Set.of(testMultiGroupAName, testMultiGroupBName)))
                .then(VirtualGroupTestAssertion.validateAdminVirtualGroupMembership(freeIpaTestClient, environmentVirtualGroups.get(),
                        Set.of(userEnvAdminA.getWorkloadUserName()), true))
                .then(VirtualGroupTestAssertion.validateAdminVirtualGroupMembership(freeIpaTestClient, environmentVirtualGroups.get(),
                        Set.of(userEnvCreatorB.getWorkloadUserName()), true))
                .validate();

        testContext
                .given(UmsTestDto.class)
                .assignTarget(EnvironmentTestDto.class.getSimpleName())
                    .withEnvironmentAdmin()
                .when(umsTestClient.unassignResourceRoleWithGroup(testMultiGroupBCrn.get()))
                    .withEnvironmentUser()
                .when(umsTestClient.assignResourceRoleWithGroup(testMultiGroupBCrn.get()))
                .then((tc, dto, client) -> {
                    environmentVirtualGroups.set(environmentUtil.getEnvironmentVirtualGroups(tc, client));
                    return dto;
                })
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.syncAll())
                .await(OperationState.COMPLETED)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.findGroups(Set.of(testMultiGroupAName, testMultiGroupBName)))
                .then(VirtualGroupTestAssertion.validateAdminVirtualGroupMembership(freeIpaTestClient, environmentVirtualGroups.get(),
                        Set.of(userEnvAdminA.getWorkloadUserName()), true))
                .then(VirtualGroupTestAssertion.validateUserVirtualGroupMembership(freeIpaTestClient, environmentVirtualGroups.get(),
                        Set.of(userEnvCreatorB.getWorkloadUserName()), true))
                .validate();

        testContext
                .given(UmsTestDto.class)
                .assignTarget(EnvironmentTestDto.class.getSimpleName())
                    .withEnvironmentAdmin()
                .when(umsTestClient.unassignResourceRoleWithGroup(testMultiGroupACrn.get()))
                    .withEnvironmentUser()
                .when(umsTestClient.unassignResourceRoleWithGroup(testMultiGroupBCrn.get()))
                .then((tc, dto, client) -> {
                    environmentVirtualGroups.set(environmentUtil.getEnvironmentVirtualGroups(tc, client));
                    return dto;
                })
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.syncAll())
                .await(OperationState.COMPLETED)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.findGroups(Set.of(testMultiGroupAName, testMultiGroupBName)))
                .then(VirtualGroupTestAssertion.validateAdminVirtualGroupMembership(freeIpaTestClient, environmentVirtualGroups.get(),
                        Set.of(userEnvAdminA.getWorkloadUserName()), false))
                .then(VirtualGroupTestAssertion.validateAdminVirtualGroupMembership(freeIpaTestClient, environmentVirtualGroups.get(),
                        Set.of(userEnvCreatorB.getWorkloadUserName()), false))
                .validate();

        testContext
                .given(UmsGroupTestDto.class)
                .when(umsTestClient.removeUserFromGroup(testMultiGroupAName, userEnvAdminA.getCrn()))
                .when(umsTestClient.removeUserFromGroup(testMultiGroupAName, userEnvCreatorB.getCrn()))
                .when(umsTestClient.removeUserFromGroup(testMultiGroupBName, userEnvAdminA.getCrn()))
                .when(umsTestClient.removeUserFromGroup(testMultiGroupBName, userEnvCreatorB.getCrn()))
                .then(UserGroupTestAssertion.validateUserGroupMembership(userEnvAdminA, testMultiGroupAName, false))
                .then(UserGroupTestAssertion.validateUserGroupMembership(userEnvCreatorB, testMultiGroupAName, false))
                .then(UserGroupTestAssertion.validateUserGroupMembership(userEnvAdminA, testMultiGroupBName, false))
                .then(UserGroupTestAssertion.validateUserGroupMembership(userEnvCreatorB, testMultiGroupBName, false))
                .validate();

        testContext
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.syncAll())
                .await(OperationState.COMPLETED)
                .given(UmsTestDto.class)
                .assignTargetByCrn(testMultiGroupACrn.get())
                    .withGroupAdmin()
                .when(umsTestClient.unAssignResourceRole(L0UserKeys.ENV_CREATOR_A))
                .then(ResourceRoleTestAssertion.validateAssignedResourceRole(userEnvCreatorA, UmsResourceRole.IAM_GROUP_ADMIN, false))
                .assignTargetByCrn(testMultiGroupBCrn.get())
                    .withGroupAdmin()
                .when(umsTestClient.unAssignResourceRole(L0UserKeys.ENV_CREATOR_A))
                .then(ResourceRoleTestAssertion.validateAssignedResourceRole(userEnvCreatorA, UmsResourceRole.IAM_GROUP_ADMIN, false))
                .validate();

        testContext
                .given(UmsGroupTestDto.class)
                .when(umsTestClient.deleteUserGroup(testMultiGroupAName))
                .then(UserGroupTestAssertion.validateUserGroupPresence(testMultiGroupAName, false))
                .when(umsTestClient.deleteUserGroup(testMultiGroupBName))
                .then(UserGroupTestAssertion.validateUserGroupPresence(testMultiGroupBName, false))
                .validate();
    }
}
