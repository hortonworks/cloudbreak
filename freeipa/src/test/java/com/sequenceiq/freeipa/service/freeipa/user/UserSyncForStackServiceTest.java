package com.sequenceiq.freeipa.service.freeipa.user;

import static com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus.COMPLETED;
import static com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus.FAILED;
import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.WorkloadCredentialsUpdateType;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsUser;
import com.sequenceiq.freeipa.service.freeipa.user.model.SyncStatusDetail;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UserSyncOptions;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersStateDifference;

@ExtendWith(MockitoExtension.class)
class UserSyncForStackServiceTest {

    private static final String ENV_CRN = "envCrn";

    private static final String ACCOUNT = "accountId";

    private static final String RESOURCE_CRN = "resourceCrn";

    private static final Stack STACK = mock(Stack.class);

    private static final FreeIpaClient FREE_IPA_CLIENT = mock(FreeIpaClient.class);

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @Mock
    private FreeIpaUsersStateProvider freeIpaUsersStateProvider;

    @Mock
    private CloudIdentitySyncService cloudIdentitySyncService;

    @Mock
    private UserSyncStateApplier stateApplier;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private UserStateDifferenceCalculator userStateDifferenceCalculator;

    @InjectMocks
    private UserSyncForStackService underTest;

    @BeforeEach
    public void init() throws FreeIpaClientException {
        when(freeIpaClientFactory.getFreeIpaClientForStack(STACK)).thenReturn(FREE_IPA_CLIENT);
        when(STACK.getEnvironmentCrn()).thenReturn(ENV_CRN);
        when(STACK.getAccountId()).thenReturn(ACCOUNT);
        when(STACK.getResourceCrn()).thenReturn(RESOURCE_CRN);
    }

    @Test
    public void testSynchronizeStackSuccessFullAtFirst() throws FreeIpaClientException {
        UmsUsersState umsUsersState = mock(UmsUsersState.class);
        UserSyncOptions options = new UserSyncOptions(true, false, WorkloadCredentialsUpdateType.FORCE_UPDATE);
        UsersState usersState = mock(UsersState.class);
        when(usersState.getUsers()).thenReturn(ImmutableSet.of());
        when(usersState.getGroups()).thenReturn(ImmutableSet.of());
        when(freeIpaUsersStateProvider.getUsersState(FREE_IPA_CLIENT)).thenReturn(usersState);
        UsersStateDifference usersStateDifference = mock(UsersStateDifference.class);
        when(userStateDifferenceCalculator.fromUmsAndIpaUsersStates(umsUsersState, usersState, options)).thenReturn(usersStateDifference);
        when(entitlementService.cloudIdentityMappingEnabled(ACCOUNT)).thenReturn(TRUE);

        SyncStatusDetail result = underTest.synchronizeStack(STACK, umsUsersState, options);

        verify(freeIpaUsersStateProvider, never()).getFilteredFreeIpaState(any(), any());
        verify(stateApplier).applyDifference(eq(umsUsersState), eq(ENV_CRN), any(), eq(usersStateDifference), eq(options), eq(FREE_IPA_CLIENT));
        verifyNoMoreInteractions(stateApplier);
        verify(cloudIdentitySyncService).syncCloudIdentities(eq(STACK), eq(umsUsersState), any());
        assertEquals(ENV_CRN, result.getEnvironmentCrn());
        assertEquals(COMPLETED, result.getStatus());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    public void testSynchronizeStackSuccessFullWithRetry() throws FreeIpaClientException {
        UmsUsersState umsUsersState = mock(UmsUsersState.class);
        UserSyncOptions options = new UserSyncOptions(true, true, WorkloadCredentialsUpdateType.FORCE_UPDATE);
        UsersState usersState = mock(UsersState.class);
        when(usersState.getUsers()).thenReturn(ImmutableSet.of());
        when(usersState.getGroups()).thenReturn(ImmutableSet.of());
        when(freeIpaUsersStateProvider.getUsersState(FREE_IPA_CLIENT)).thenReturn(usersState);
        UsersStateDifference usersStateDifference = mock(UsersStateDifference.class);
        when(userStateDifferenceCalculator.fromUmsAndIpaUsersStates(umsUsersState, usersState, options)).thenReturn(usersStateDifference);
        when(userStateDifferenceCalculator.usersStateDifferenceChanged(any(), any())).thenReturn(TRUE);
        when(entitlementService.cloudIdentityMappingEnabled(ACCOUNT)).thenReturn(TRUE);
        doAnswer(invocation -> {
            Multimap<String, String> warnings = invocation.getArgument(2, Multimap.class);
            warnings.put(ENV_CRN, "failed");
            return null;
        })
                .doNothing()
                .when(stateApplier).applyDifference(eq(umsUsersState), eq(ENV_CRN), any(), eq(usersStateDifference), eq(options), eq(FREE_IPA_CLIENT));

        SyncStatusDetail result = underTest.synchronizeStack(STACK, umsUsersState, options);

        verify(freeIpaUsersStateProvider, never()).getFilteredFreeIpaState(any(), any());
        verify(cloudIdentitySyncService).syncCloudIdentities(eq(STACK), eq(umsUsersState), any());
        assertEquals(ENV_CRN, result.getEnvironmentCrn());
        assertEquals(COMPLETED, result.getStatus());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    public void testSynchronizeStackSuccessPartialAtFirst() throws FreeIpaClientException {
        UmsUsersState umsUsersState = mock(UmsUsersState.class);
        when(umsUsersState.getRequestedWorkloadUsernames()).thenReturn(ImmutableSet.of("user1", "user2"));
        UserSyncOptions options = new UserSyncOptions(false, false, WorkloadCredentialsUpdateType.FORCE_UPDATE);
        UsersState usersState = mock(UsersState.class);
        when(usersState.getUsers()).thenReturn(ImmutableSet.of());
        when(usersState.getGroups()).thenReturn(ImmutableSet.of());
        when(freeIpaUsersStateProvider.getFilteredFreeIpaState(FREE_IPA_CLIENT, Set.of("user1", "user2"))).thenReturn(usersState);
        UsersStateDifference usersStateDifference = mock(UsersStateDifference.class);
        when(userStateDifferenceCalculator.fromUmsAndIpaUsersStates(umsUsersState, usersState, options)).thenReturn(usersStateDifference);

        SyncStatusDetail result = underTest.synchronizeStack(STACK, umsUsersState, options);

        verify(freeIpaUsersStateProvider, never()).getUsersState(any());
        verify(stateApplier).applyDifference(eq(umsUsersState), eq(ENV_CRN), any(), eq(usersStateDifference), eq(options), eq(FREE_IPA_CLIENT));
        verifyNoMoreInteractions(stateApplier);
        verifyNoInteractions(cloudIdentitySyncService);
        assertEquals(ENV_CRN, result.getEnvironmentCrn());
        assertEquals(COMPLETED, result.getStatus());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    public void testSynchronizeStackFailsPartial() throws FreeIpaClientException {
        UmsUsersState umsUsersState = mock(UmsUsersState.class);
        when(umsUsersState.getRequestedWorkloadUsernames()).thenReturn(ImmutableSet.of("user1", "user2"));
        UserSyncOptions options = new UserSyncOptions(false, false, WorkloadCredentialsUpdateType.FORCE_UPDATE);
        UsersState usersState = mock(UsersState.class);
        when(usersState.getUsers()).thenReturn(ImmutableSet.of());
        when(usersState.getGroups()).thenReturn(ImmutableSet.of());
        when(freeIpaUsersStateProvider.getFilteredFreeIpaState(FREE_IPA_CLIENT, Set.of("user1", "user2"))).thenReturn(usersState);
        UsersStateDifference usersStateDifference = mock(UsersStateDifference.class);
        when(userStateDifferenceCalculator.fromUmsAndIpaUsersStates(umsUsersState, usersState, options)).thenReturn(usersStateDifference);
        doAnswer(invocation -> {
            Multimap<String, String> warnings = invocation.getArgument(2, Multimap.class);
            warnings.put(ENV_CRN, "failed");
            return null;
        })
                .when(stateApplier).applyDifference(eq(umsUsersState), eq(ENV_CRN), any(), eq(usersStateDifference), eq(options), eq(FREE_IPA_CLIENT));

        SyncStatusDetail result = underTest.synchronizeStack(STACK, umsUsersState, options);

        verify(freeIpaUsersStateProvider, never()).getUsersState(any());
        verify(stateApplier).applyDifference(eq(umsUsersState), eq(ENV_CRN), any(), eq(usersStateDifference), eq(options), eq(FREE_IPA_CLIENT));
        verifyNoMoreInteractions(stateApplier);
        verifyNoInteractions(cloudIdentitySyncService);
        assertEquals(ENV_CRN, result.getEnvironmentCrn());
        assertEquals(FAILED, result.getStatus());
        assertFalse(result.getWarnings().isEmpty());
    }

    @Test
    public void testSynchronizeStackFailsToGetClient() throws FreeIpaClientException {
        UmsUsersState umsUsersState = mock(UmsUsersState.class);
        UserSyncOptions options = new UserSyncOptions(false, false, WorkloadCredentialsUpdateType.FORCE_UPDATE);
        when(freeIpaClientFactory.getFreeIpaClientForStack(STACK)).thenThrow(new FreeIpaClientException("potty"));

        SyncStatusDetail result = underTest.synchronizeStack(STACK, umsUsersState, options);

        assertEquals(ENV_CRN, result.getEnvironmentCrn());
        assertEquals(FAILED, result.getStatus());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    public void testSynchronizeStackForDeleteUserEmpty() throws FreeIpaClientException {
        UsersState usersState = mock(UsersState.class);
        when(usersState.getUsers()).thenReturn(ImmutableSet.of());
        when(usersState.getGroups()).thenReturn(ImmutableSet.of());
        when(freeIpaUsersStateProvider.getFilteredFreeIpaState(FREE_IPA_CLIENT, Set.of("deleteMe"))).thenReturn(usersState);

        SyncStatusDetail result = underTest.synchronizeStackForDeleteUser(STACK, "deleteMe");

        verifyNoInteractions(stateApplier);
        assertEquals(ENV_CRN, result.getEnvironmentCrn());
        assertEquals(COMPLETED, result.getStatus());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    public void testSynchronizeStackForDeleteUser() throws FreeIpaClientException {
        UsersState usersState = mock(UsersState.class);
        when(usersState.getUsers()).thenReturn(ImmutableSet.of(new FmsUser().withName("deleteMe")));
        when(usersState.getGroups()).thenReturn(ImmutableSet.of());
        when(usersState.getGroupMembership()).thenReturn(ImmutableMultimap.of("deleteMe", "group"));
        when(freeIpaUsersStateProvider.getFilteredFreeIpaState(FREE_IPA_CLIENT, Set.of("deleteMe"))).thenReturn(usersState);
        UsersStateDifference usersStateDifference = mock(UsersStateDifference.class);
        ArgumentCaptor<Collection<String>> captor = ArgumentCaptor.forClass(Collection.class);
        when(userStateDifferenceCalculator.forDeletedUser(eq("deleteMe"), captor.capture())).thenReturn(usersStateDifference);

        SyncStatusDetail result = underTest.synchronizeStackForDeleteUser(STACK, "deleteMe");

        verify(stateApplier).applyStateDifferenceToIpa(eq(ENV_CRN), eq(FREE_IPA_CLIENT), eq(usersStateDifference), any(), eq(false));
        assertTrue(captor.getValue().contains("group"));
        assertEquals(1, captor.getValue().size());
        assertEquals(ENV_CRN, result.getEnvironmentCrn());
        assertEquals(COMPLETED, result.getStatus());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    public void testSynchronizeStackForDeleteUserFailure() throws FreeIpaClientException {
        when(freeIpaClientFactory.getFreeIpaClientForStack(STACK)).thenThrow(new FreeIpaClientException("totty"));

        SyncStatusDetail result = underTest.synchronizeStackForDeleteUser(STACK, "deleteMe");

        verifyNoInteractions(stateApplier);
        assertEquals(ENV_CRN, result.getEnvironmentCrn());
        assertEquals(FAILED, result.getStatus());
        assertTrue(result.getWarnings().isEmpty());
    }
}