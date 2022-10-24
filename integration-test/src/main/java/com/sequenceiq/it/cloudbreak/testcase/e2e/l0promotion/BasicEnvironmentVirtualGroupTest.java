package com.sequenceiq.it.cloudbreak.testcase.e2e.l0promotion;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.auth.altus.UmsVirtualGroupRight;
import com.sequenceiq.cloudbreak.auth.altus.service.UmsResourceRole;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
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

public class BasicEnvironmentVirtualGroupTest extends AbstractE2ETest {

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
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private EnvironmentUtil environmentUtil;

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
        AtomicReference<Map<UmsVirtualGroupRight, String>> environmentVirtualGroups = new AtomicReference<>();
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
                .when(umsTestClient.assignResourceRole(L0UserKeys.ENV_ADMIN_A, regionAwareInternalCrnGeneratorFactory))
                .withEnvironmentUser()
                .when(umsTestClient.assignResourceRole(L0UserKeys.ENV_CREATOR_B, regionAwareInternalCrnGeneratorFactory))
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
                .when(umsTestClient.assignResourceRole(L0UserKeys.ENV_CREATOR_B, regionAwareInternalCrnGeneratorFactory))
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
                .when(umsTestClient.unAssignResourceRole(L0UserKeys.ENV_ADMIN_A, regionAwareInternalCrnGeneratorFactory))
                .withEnvironmentUser()
                .when(umsTestClient.unAssignResourceRole(L0UserKeys.ENV_CREATOR_B, regionAwareInternalCrnGeneratorFactory))
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
                .when(umsTestClient.assignResourceRole(L0UserKeys.ENV_CREATOR_A, regionAwareInternalCrnGeneratorFactory))
                .then(ResourceRoleTestAssertion.validateAssignedResourceRole(userEnvCreatorA, UmsResourceRole.IAM_GROUP_ADMIN, true,
                        regionAwareInternalCrnGeneratorFactory))
                .assignTargetByCrn(userGroupCrn)
                .withGroupAdmin()
                .when(umsTestClient.assignResourceRole(L0UserKeys.ENV_CREATOR_A, regionAwareInternalCrnGeneratorFactory))
                .then(ResourceRoleTestAssertion.validateAssignedResourceRole(userEnvCreatorA, UmsResourceRole.IAM_GROUP_ADMIN, true,
                        regionAwareInternalCrnGeneratorFactory))
                .given(UmsGroupTestDto.class)
                .when(umsTestClient.addUserToGroup(adminGroupName, userEnvAdminA.getCrn(), regionAwareInternalCrnGeneratorFactory))
                .when(umsTestClient.addUserToGroup(userGroupName, userEnvCreatorB.getCrn(), regionAwareInternalCrnGeneratorFactory))
                .then(UserGroupTestAssertion.validateUserGroupMembership(userEnvAdminA, adminGroupName, true,
                        regionAwareInternalCrnGeneratorFactory))
                .then(UserGroupTestAssertion.validateUserGroupMembership(userEnvCreatorB, userGroupName, true,
                        regionAwareInternalCrnGeneratorFactory))
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
                .when(umsTestClient.assignResourceRoleWithGroup(adminGroupCrn, regionAwareInternalCrnGeneratorFactory))
                .withEnvironmentUser()
                .when(umsTestClient.assignResourceRoleWithGroup(userGroupCrn, regionAwareInternalCrnGeneratorFactory))
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
                .when(umsTestClient.removeUserFromGroup(adminGroupName, userEnvAdminA.getCrn(), regionAwareInternalCrnGeneratorFactory))
                .when(umsTestClient.removeUserFromGroup(userGroupName, userEnvCreatorB.getCrn(), regionAwareInternalCrnGeneratorFactory))
                .then(UserGroupTestAssertion.validateUserGroupMembership(userEnvAdminA, adminGroupName, false,
                        regionAwareInternalCrnGeneratorFactory))
                .then(UserGroupTestAssertion.validateUserGroupMembership(userEnvCreatorB, userGroupName, false,
                        regionAwareInternalCrnGeneratorFactory))
                .validate();

        testContext
                .given(UmsTestDto.class)
                .assignTargetByCrn(adminGroupCrn)
                .withGroupAdmin()
                .when(umsTestClient.unAssignResourceRole(L0UserKeys.ENV_CREATOR_A, regionAwareInternalCrnGeneratorFactory))
                .then(ResourceRoleTestAssertion.validateAssignedResourceRole(userEnvCreatorA, UmsResourceRole.IAM_GROUP_ADMIN, false,
                        regionAwareInternalCrnGeneratorFactory))
                .assignTargetByCrn(userGroupCrn)
                .withGroupAdmin()
                .when(umsTestClient.unAssignResourceRole(L0UserKeys.ENV_CREATOR_A, regionAwareInternalCrnGeneratorFactory))
                .then(ResourceRoleTestAssertion.validateAssignedResourceRole(userEnvCreatorA, UmsResourceRole.IAM_GROUP_ADMIN, false,
                        regionAwareInternalCrnGeneratorFactory))
                .validate();
    }
}
