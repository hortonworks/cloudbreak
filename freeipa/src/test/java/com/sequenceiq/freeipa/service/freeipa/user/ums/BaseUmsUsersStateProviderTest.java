package com.sequenceiq.freeipa.service.freeipa.user.ums;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.freeipa.service.freeipa.user.UserSyncConstants;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsGroup;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsUser;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UserMetadata;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;

@ExtendWith(MockitoExtension.class)
class BaseUmsUsersStateProviderTest {

    @Mock
    protected GrpcUmsClient grpcUmsClient;

    protected final UserSyncTestData testData = new UserSyncTestData();

    protected void setupServicePrincipals() {
        when(grpcUmsClient.listServicePrincipalCloudIdentities(
                eq(INTERNAL_ACTOR_CRN), eq(testData.getAccountId()), eq(testData.getEnvironmentCrn()), any(Optional.class)))
                .thenReturn(testData.getServicePrincipalCloudIdentities());
    }

    protected void verifyUmsUsersStateBuilderMap(Map<String, UmsUsersState> umsUsersStateMap) {
        assertEquals(1, umsUsersStateMap.size());
        UmsUsersState state = umsUsersStateMap.get(testData.getEnvironmentCrn());
        assertNotNull(state);

        // Add the internal group to the expected groups and wags
        assertEquals(testData.getGroups().size() + testData.getWagsForThisEnvironment().size() + 1,
                state.getUsersState().getGroups().size());
        List<String> groupNames = state.getUsersState().getGroups()
                .stream().map(FmsGroup::getName).collect(Collectors.toList());
        assertTrue(groupNames.containsAll(
                testData.getGroups().stream().map(UserManagementProto.Group::getGroupName).collect(Collectors.toList())));
        assertTrue(groupNames.containsAll(
                testData.getWagsForThisEnvironment().stream()
                        .map(UserManagementProto.WorkloadAdministrationGroup::getWorkloadAdministrationGroupName)
                        .collect(Collectors.toList())));
        assertEquals(
                testData.getAllWags().stream()
                        .map(UserManagementProto.WorkloadAdministrationGroup::getWorkloadAdministrationGroupName)
                        .collect(Collectors.toSet()),
                state.getWorkloadAdministrationGroups().stream()
                        .map(FmsGroup::getName)
                        .collect(Collectors.toSet()));

        assertEquals(Stream.concat(testData.getUsers().stream().map(UserManagementProto.User::getWorkloadUsername),
                testData.getMachineUsers().stream()
                        .map(UserManagementProto.MachineUser::getWorkloadUsername))
                        .collect(Collectors.toSet()),
                state.getRequestedWorkloadUsernames());

        // users including rights, group membership, wags
        // machine users including rights, group membership, wags
        UsersState usersState = state.getUsersState();
        Set<String> workloadUsersWithAccess = usersState.getUsers().stream()
                .map(FmsUser::getName)
                .collect(Collectors.toSet());
        Multimap<String, String> groupsPerMember = Multimaps.invertFrom(usersState.getGroupMembership(),
                ArrayListMultimap.<String, String>create());
        testData.getUsers().forEach(u ->
                verifyActor(u.getCrn(), u.getWorkloadUsername(), workloadUsersWithAccess,
                        groupsPerMember.get(u.getWorkloadUsername()),
                        state.getUsersWorkloadCredentialMap().get(u.getWorkloadUsername()),
                        usersState.getUserMetadataMap().get(u.getWorkloadUsername())));
        testData.getMachineUsers().forEach(u ->
                verifyActor(u.getCrn(), u.getWorkloadUsername(), workloadUsersWithAccess,
                        groupsPerMember.get(u.getWorkloadUsername()),
                        state.getUsersWorkloadCredentialMap().get(u.getWorkloadUsername()),
                        usersState.getUserMetadataMap().get(u.getWorkloadUsername())));

        assertEquals(testData.getServicePrincipalCloudIdentities(), state.getServicePrincipalCloudIdentities());
    }

    private void verifyActor(
            String actorCrn, String workloadUsername,
            Set<String> workloadUsersWithAccess,
            Collection<String> actualGroups,
            WorkloadCredential workloadCredential,
            UserMetadata userMetadata) {
        if (testData.getMemberCrnToActorRights().get(actorCrn).get(UserSyncConstants.RIGHTS.get(0))) {
            assertTrue(workloadUsersWithAccess.contains(workloadUsername));
            Map<String, Boolean> expectedGroupMembership = testData.getMemberCrnToGroupMembership().get(actorCrn);
            testData.getGroups().forEach(g -> {
                if (expectedGroupMembership.get(g.getCrn())) {
                    assertTrue(actualGroups.contains(g.getGroupName()));
                } else {
                    assertFalse(actualGroups.contains(g.getGroupName()));
                }
            });
            Map<String, Boolean> expectedWagMembership = testData.getMemberCrnToWagMembership().get(actorCrn);
            testData.getWagsForThisEnvironment().forEach(wag -> {
                if (expectedWagMembership.get(wag.getWorkloadAdministrationGroupName())) {
                    assertTrue(actualGroups.contains(wag.getWorkloadAdministrationGroupName()));
                } else {
                    assertFalse(actualGroups.contains(wag.getWorkloadAdministrationGroupName()));
                }
            });
            testData.getWagsForOtherEnvironment().forEach(wag ->
                    assertFalse(actualGroups.contains(wag.getWorkloadAdministrationGroupName())));
            verifyWorkloadCredential(actorCrn, workloadCredential);
            assertEquals(actorCrn, userMetadata.getCrn());
            assertEquals(workloadCredential.getVersion(), userMetadata.getWorkloadCredentialsVersion());
        } else {
            assertFalse(workloadUsersWithAccess.contains(workloadUsername));
            assertNull(workloadCredential);
            assertNull(userMetadata);
        }
    }

    private void verifyWorkloadCredential(String actorCrn, WorkloadCredential workloadCredential) {
        assertNotNull(workloadCredential);
        UserManagementProto.GetActorWorkloadCredentialsResponse expected =
                testData.getMemberCrnToWorkloadCredentials().get(actorCrn);
        assertEquals(expected.getPasswordHash(), workloadCredential.getHashedPassword());
        assertEquals(expected.getKerberosKeysList(), workloadCredential.getKeys());
        assertEquals(expected.getSshPublicKeyList(), workloadCredential.getSshPublicKeys());
        assertEquals(expected.getWorkloadCredentialsVersion(), workloadCredential.getVersion());
    }
}