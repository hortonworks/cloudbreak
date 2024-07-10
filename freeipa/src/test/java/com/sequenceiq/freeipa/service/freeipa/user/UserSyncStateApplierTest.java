package com.sequenceiq.freeipa.service.freeipa.user;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.Config;
import com.sequenceiq.freeipa.service.freeipa.WorkloadCredentialService;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsGroup;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsUser;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UserMetadata;
import com.sequenceiq.freeipa.service.freeipa.user.model.UserSyncOptions;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersStateDifference;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredentialUpdate;

@ExtendWith(MockitoExtension.class)
class UserSyncStateApplierTest {

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:"
            + ACCOUNT_ID + ":environment:" + UUID.randomUUID();

    private static final long STACK_ID = 3L;

    @Mock
    private FreeIpaClient freeIpaClient;

    @Mock
    private UserSyncOperations operations;

    @Mock
    private WorkloadCredentialService workloadCredentialService;

    @Mock
    private UserSyncGroupAddMemberOperations groupAddMemberOperations;

    @InjectMocks
    private UserSyncStateApplier underTest;

    @Test
    void testApplyStateDifferenceToIpa() throws FreeIpaClientException, TimeoutException {
        UmsUsersState umsUsersState = UmsUsersState.newBuilder()
                .setUsersState(mock(UsersState.class))
                .setGroupsExceedingThreshold(ImmutableSet.of("largeGroup"))
                .build();

        Multimap<String, String> warnings = ArrayListMultimap.create();

        UsersStateDifference usersStateDifference = createStateDiff(umsUsersState);

        underTest.applyStateDifferenceToIpa(umsUsersState, ENV_CRN, freeIpaClient, usersStateDifference, warnings::put, true);

        verifyOperationsCalled(umsUsersState, usersStateDifference);
    }

    @Test
    public void testApplyDifferenceNoPasswordHashSupport() throws FreeIpaClientException, TimeoutException {
        UmsUsersState umsUsersState = UmsUsersState.newBuilder()
                .setUsersState(mock(UsersState.class))
                .build();
        UserSyncOptions userSyncOptions = mock(UserSyncOptions.class);
        UsersStateDifference usersStateDifference = createStateDiff(umsUsersState);
        Multimap<String, String> warnings = ArrayListMultimap.create();
        when(userSyncOptions.isFmsToFreeIpaBatchCallEnabled()).thenReturn(Boolean.TRUE);
        when(freeIpaClient.getConfig()).thenReturn(new Config());

        underTest.applyDifference(umsUsersState, ENV_CRN, warnings, usersStateDifference, userSyncOptions, freeIpaClient, STACK_ID);

        verifyNoInteractions(workloadCredentialService);
    }

    @Test
    public void testApplyDifferenceWithPasswordHashSupport() throws FreeIpaClientException, TimeoutException {
        UsersState usersState = UsersState.newBuilder()
                .addUserMetadata("userToUpdate1", new UserMetadata("userToUpdate1Crn", 1L))
                .addUserMetadata("userToUpdate2", new UserMetadata("userToUpdate2Crn", 2L))
                .build();
        WorkloadCredential workloadCredential1 = mock(WorkloadCredential.class);
        WorkloadCredential workloadCredential2 = mock(WorkloadCredential.class);
        UmsUsersState umsUsersState = UmsUsersState.newBuilder()
                .setUsersState(usersState)
                .addWorkloadCredentials("userToUpdate1", workloadCredential1)
                .addWorkloadCredentials("userToUpdate2", workloadCredential2)
                .build();
        UserSyncOptions userSyncOptions = mock(UserSyncOptions.class);
        UsersStateDifference usersStateDifference = createStateDiff(umsUsersState);
        Multimap<String, String> warnings = ArrayListMultimap.create();
        when(userSyncOptions.isFmsToFreeIpaBatchCallEnabled()).thenReturn(Boolean.TRUE);
        Config config = new Config();
        config.setIpauserobjectclasses(Set.of("cdpUserAttr"));
        when(freeIpaClient.getConfig()).thenReturn(config);

        underTest.applyDifference(umsUsersState, ENV_CRN, warnings, usersStateDifference, userSyncOptions, freeIpaClient, STACK_ID);

        ArgumentCaptor<Set<WorkloadCredentialUpdate>> credentialUpdateCaptor = ArgumentCaptor.forClass(Set.class);
        verify(workloadCredentialService).setWorkloadCredentials(eq(userSyncOptions), eq(freeIpaClient), credentialUpdateCaptor.capture(), any(), eq(STACK_ID));
        Set<WorkloadCredentialUpdate> workloadCredentialUpdates = credentialUpdateCaptor.getValue();
        assertThat(workloadCredentialUpdates, allOf(
                hasItem(allOf(
                        hasProperty("username", is("userToUpdate1")),
                        hasProperty("userCrn", is("userToUpdate1Crn")),
                        hasProperty("workloadCredential", is(workloadCredential1))
                )),
                hasItem(allOf(
                        hasProperty("username", is("userToUpdate2")),
                        hasProperty("userCrn", is("userToUpdate2Crn")),
                        hasProperty("workloadCredential", is(workloadCredential2))
                ))
        ));
    }

    private void verifyOperationsCalled(UmsUsersState umsUsersState, UsersStateDifference usersStateDifference) throws FreeIpaClientException, TimeoutException {
        verify(operations).addGroups(eq(true), eq(freeIpaClient), eq(usersStateDifference.getGroupsToAdd()), any());
        verify(operations).addUsers(eq(true), eq(freeIpaClient), eq(usersStateDifference.getUsersToAdd()), any());
        verify(operations).disableUsers(eq(true), eq(freeIpaClient), eq(usersStateDifference.getUsersToDisable()), any());
        verify(operations).enableUsers(eq(true), eq(freeIpaClient), eq(usersStateDifference.getUsersToEnable()), any());
        verify(groupAddMemberOperations).addMembersToSmallGroups(eq(true), eq(freeIpaClient), eq(usersStateDifference.getGroupMembershipToAdd()),
                eq(umsUsersState.getGroupsExceedingThreshold()), any());
        verify(groupAddMemberOperations).addMembersToLargeGroups(eq(freeIpaClient), eq(usersStateDifference.getGroupMembershipToAdd()),
                eq(umsUsersState.getGroupsExceedingThreshold()), any());
        verify(operations).removeUsersFromGroups(eq(true), eq(freeIpaClient), eq(usersStateDifference.getGroupMembershipToRemove()), any());
        verify(operations).removeUsers(eq(true), eq(freeIpaClient), eq(usersStateDifference.getUsersToRemove()), any());
        verify(operations).removeGroups(eq(true), eq(freeIpaClient), eq(usersStateDifference.getGroupsToRemove()), any());
        verifyNoMoreInteractions(operations);
        verifyNoMoreInteractions(groupAddMemberOperations);
    }

    private UsersStateDifference createStateDiff(UmsUsersState umsUsersState) {
        ImmutableSet.Builder<FmsGroup> groupsToAdd = ImmutableSet.builder();
        FmsGroup groupToAdd1 = new FmsGroup().withName("groupToAdd1");
        groupsToAdd.add(groupToAdd1);
        FmsGroup groupToAdd2 = new FmsGroup().withName("groupToAdd2");
        groupsToAdd.add(groupToAdd2);
        umsUsersState.getGroupsExceedingThreshold()
                .forEach(groupName -> groupsToAdd.add(new FmsGroup().withName(groupName)));
        FmsGroup groupToRemove1 = new FmsGroup().withName("groupToRemove1");
        FmsGroup groupToRemove2 = new FmsGroup().withName("groupToRemove2");
        FmsUser userToAdd1 = new FmsUser().withName("userToAdd1").withFirstName("clark").withLastName("kent");
        FmsUser userToAdd2 = new FmsUser().withName("userToAdd2").withFirstName("peter").withLastName("parker");
        String userToRemove1 = "userToRemove1";
        String userToRemove2 = "userToRemove2";
        String userToDisable1 = "userToDisable1";
        String userToDisable2 = "userToDisable2";
        String userToEnable1 = "userToEnable1";
        String userToEnable2 = "userToEnable2";
        ImmutableMultimap.Builder groupMembershipsToAdd = ImmutableMultimap.<String, String>builder();
        groupMembershipsToAdd
                .put(groupToAdd1.getName(), userToAdd1.getName())
                .put(groupToAdd2.getName(), userToAdd2.getName());
        umsUsersState.getGroupsExceedingThreshold()
                .forEach(groupName -> groupMembershipsToAdd.put(groupName, userToAdd1.getName()));

        return new UsersStateDifference(
                groupsToAdd.build(),
                ImmutableSet.of(groupToRemove1, groupToRemove2),
                ImmutableSet.of(userToAdd1, userToAdd2),
                ImmutableSet.of("userToUpdate1", "userToUpdate2"),
                ImmutableSet.of(userToRemove1, userToRemove2),
                groupMembershipsToAdd.build(),
                ImmutableMultimap.<String, String>builder()
                        .put(groupToRemove1.getName(), userToRemove1)
                        .put(groupToRemove2.getName(), userToRemove2)
                        .build(),
                ImmutableSet.of(userToDisable1, userToDisable2),
                ImmutableSet.of(userToEnable1, userToEnable2)
        );
    }

}