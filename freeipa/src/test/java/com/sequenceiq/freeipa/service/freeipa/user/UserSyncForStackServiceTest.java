package com.sequenceiq.freeipa.service.freeipa.user;

import static com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus.COMPLETED;
import static com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus.FAILED;
import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.WorkloadCredentialsUpdateType;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.entity.projection.StackUserSyncView;
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

    private static final StackUserSyncView STACK = mock(StackUserSyncView.class);

    private static final FreeIpaClient FREE_IPA_CLIENT = mock(FreeIpaClient.class);

    private static final String ERROR_MESSAGE = "error message";

    private static final String OPERATION_ID = "operationId";

    private static final long STACK_ID = 1L;

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

    @Mock
    private SudoRuleService sudoRuleService;

    @Mock
    private AuthDistributorService authDistributorService;

    @InjectMocks
    private UserSyncForStackService underTest;

    @BeforeEach
    public void init() throws FreeIpaClientException {
        when(freeIpaClientFactory.getFreeIpaClientForStackId(STACK_ID)).thenReturn(FREE_IPA_CLIENT);
        when(STACK.environmentCrn()).thenReturn(ENV_CRN);
        when(STACK.accountId()).thenReturn(ACCOUNT);
        when(STACK.resourceCrn()).thenReturn(RESOURCE_CRN);
        when(STACK.id()).thenReturn(STACK_ID);
    }

    @Test
    public void testSynchronizeStackSuccessFullAtFirst() throws Exception {
        UmsUsersState umsUsersState = mock(UmsUsersState.class);
        UserSyncOptions options = UserSyncOptions.newBuilder()
                .fullSync(true)
                .fmsToFreeIpaBatchCallEnabled(false)
                .workloadCredentialsUpdateType(WorkloadCredentialsUpdateType.FORCE_UPDATE)
                .build();
        UsersState usersState = mock(UsersState.class);
        when(usersState.getUsers()).thenReturn(ImmutableSet.of());
        when(usersState.getGroups()).thenReturn(ImmutableSet.of());
        when(freeIpaUsersStateProvider.getUsersState(FREE_IPA_CLIENT, false)).thenReturn(usersState);
        UsersStateDifference usersStateDifference = mock(UsersStateDifference.class);
        when(userStateDifferenceCalculator.fromUmsAndIpaUsersStates(eq(umsUsersState), eq(usersState), eq(options), any()))
                .thenReturn(usersStateDifference);
        when(entitlementService.cloudIdentityMappingEnabled(ACCOUNT)).thenReturn(TRUE);
        when(entitlementService.isEnvironmentPrivilegedUserEnabled(ACCOUNT)).thenReturn(TRUE);

        SyncStatusDetail result = underTest.synchronizeStack(STACK, umsUsersState, options, OPERATION_ID);

        verify(freeIpaUsersStateProvider, never()).getFilteredFreeIpaState(any(), any());
        verify(stateApplier).applyDifference(eq(umsUsersState), eq(ENV_CRN), any(), eq(usersStateDifference), eq(options), eq(FREE_IPA_CLIENT), eq(STACK_ID));
        verifyNoMoreInteractions(stateApplier);
        verify(cloudIdentitySyncService).syncCloudIdentities(eq(STACK), eq(umsUsersState), any());
        verify(sudoRuleService).setupSudoRule(STACK, FREE_IPA_CLIENT);
        verify(authDistributorService).updateAuthViewForEnvironment(eq(ENV_CRN), any(), eq(ACCOUNT), eq(OPERATION_ID));
        assertEquals(ENV_CRN, result.getEnvironmentCrn());
        assertEquals(COMPLETED, result.getStatus());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    public void testSynchronizeFailsToSetupSudoRules() throws Exception {
        UmsUsersState umsUsersState = mock(UmsUsersState.class);
        UserSyncOptions options = UserSyncOptions.newBuilder()
                .fullSync(true)
                .fmsToFreeIpaBatchCallEnabled(false)
                .workloadCredentialsUpdateType(WorkloadCredentialsUpdateType.FORCE_UPDATE)
                .build();
        UsersState usersState = mock(UsersState.class);
        when(usersState.getUsers()).thenReturn(ImmutableSet.of());
        when(usersState.getGroups()).thenReturn(ImmutableSet.of());
        when(freeIpaUsersStateProvider.getUsersState(FREE_IPA_CLIENT, false)).thenReturn(usersState);
        UsersStateDifference usersStateDifference = mock(UsersStateDifference.class);
        when(userStateDifferenceCalculator.fromUmsAndIpaUsersStates(eq(umsUsersState), eq(usersState), eq(options), any()))
                .thenReturn(usersStateDifference);
        when(entitlementService.cloudIdentityMappingEnabled(ACCOUNT)).thenReturn(TRUE);
        when(entitlementService.isEnvironmentPrivilegedUserEnabled(ACCOUNT)).thenReturn(TRUE);
        doThrow(new Exception(ERROR_MESSAGE)).when(sudoRuleService).setupSudoRule(STACK, FREE_IPA_CLIENT);

        SyncStatusDetail result = underTest.synchronizeStack(STACK, umsUsersState, options, OPERATION_ID);

        verify(freeIpaUsersStateProvider, never()).getFilteredFreeIpaState(any(), any());
        verify(stateApplier).applyDifference(eq(umsUsersState), eq(ENV_CRN), any(), eq(usersStateDifference), eq(options), eq(FREE_IPA_CLIENT), eq(STACK_ID));
        verifyNoMoreInteractions(stateApplier);
        verify(cloudIdentitySyncService).syncCloudIdentities(eq(STACK), eq(umsUsersState), any());
        verify(authDistributorService, never()).updateAuthViewForEnvironment(eq(ENV_CRN), any(), eq(ACCOUNT), eq(OPERATION_ID));
        assertEquals(ENV_CRN, result.getEnvironmentCrn());
        assertEquals(FAILED, result.getStatus());
        assertTrue(result.getWarnings().get(ENV_CRN).contains(ERROR_MESSAGE));
    }

    @Test
    public void testSynchronizeStackSuccessFullWithRetry() throws Exception {
        UmsUsersState umsUsersState = mock(UmsUsersState.class);
        UserSyncOptions options = UserSyncOptions.newBuilder()
                .fullSync(true)
                .fmsToFreeIpaBatchCallEnabled(true)
                .workloadCredentialsUpdateType(WorkloadCredentialsUpdateType.FORCE_UPDATE)
                .build();
        UsersState usersState = mock(UsersState.class);
        when(usersState.getUsers()).thenReturn(ImmutableSet.of());
        when(usersState.getGroups()).thenReturn(ImmutableSet.of());
        when(freeIpaUsersStateProvider.getUsersState(FREE_IPA_CLIENT, false)).thenReturn(usersState);
        UsersStateDifference usersStateDifference = mock(UsersStateDifference.class);
        when(userStateDifferenceCalculator.fromUmsAndIpaUsersStates(eq(umsUsersState), eq(usersState), eq(options), any()))
                .thenReturn(usersStateDifference);
        when(userStateDifferenceCalculator.usersStateDifferenceChanged(any(), any())).thenReturn(TRUE);
        when(entitlementService.cloudIdentityMappingEnabled(ACCOUNT)).thenReturn(TRUE);
        when(entitlementService.isEnvironmentPrivilegedUserEnabled(ACCOUNT)).thenReturn(TRUE);
        doAnswer(invocation -> {
            Multimap<String, String> warnings = invocation.getArgument(2, Multimap.class);
            warnings.put(ENV_CRN, "failed");
            return null;
        })
                .doNothing()
                .when(stateApplier).applyDifference(eq(umsUsersState), eq(ENV_CRN), any(), eq(usersStateDifference), eq(options), eq(FREE_IPA_CLIENT),
                        eq(STACK_ID));

        SyncStatusDetail result = underTest.synchronizeStack(STACK, umsUsersState, options, OPERATION_ID);

        verify(freeIpaUsersStateProvider, never()).getFilteredFreeIpaState(any(), any());
        verify(cloudIdentitySyncService).syncCloudIdentities(eq(STACK), eq(umsUsersState), any());
        verify(sudoRuleService).setupSudoRule(STACK, FREE_IPA_CLIENT);
        verify(authDistributorService).updateAuthViewForEnvironment(eq(ENV_CRN), any(), eq(ACCOUNT), eq(OPERATION_ID));
        assertEquals(ENV_CRN, result.getEnvironmentCrn());
        assertEquals(COMPLETED, result.getStatus());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    public void testSynchronizeStackSuccessPartialAtFirst() throws FreeIpaClientException, TimeoutException {
        UmsUsersState umsUsersState = mock(UmsUsersState.class);
        when(umsUsersState.getRequestedWorkloadUsernames()).thenReturn(ImmutableSet.of("user1", "user2"));
        UserSyncOptions options = UserSyncOptions.newBuilder()
                .fullSync(false)
                .fmsToFreeIpaBatchCallEnabled(false)
                .workloadCredentialsUpdateType(WorkloadCredentialsUpdateType.FORCE_UPDATE)
                .build();
        UsersState usersState = mock(UsersState.class);
        when(usersState.getUsers()).thenReturn(ImmutableSet.of());
        when(usersState.getGroups()).thenReturn(ImmutableSet.of());
        when(freeIpaUsersStateProvider.getFilteredFreeIpaState(FREE_IPA_CLIENT, Set.of("user1", "user2"))).thenReturn(usersState);
        UsersStateDifference usersStateDifference = mock(UsersStateDifference.class);
        when(userStateDifferenceCalculator.fromUmsAndIpaUsersStates(eq(umsUsersState), eq(usersState), eq(options), any()))
                .thenReturn(usersStateDifference);

        SyncStatusDetail result = underTest.synchronizeStack(STACK, umsUsersState, options, OPERATION_ID);

        verify(freeIpaUsersStateProvider, never()).getUsersState(any(), anyBoolean());
        verify(stateApplier).applyDifference(eq(umsUsersState), eq(ENV_CRN), any(), eq(usersStateDifference), eq(options), eq(FREE_IPA_CLIENT), eq(STACK_ID));
        verifyNoMoreInteractions(stateApplier);
        verifyNoInteractions(cloudIdentitySyncService);
        verifyNoInteractions(sudoRuleService);
        verifyNoInteractions(authDistributorService);
        assertEquals(ENV_CRN, result.getEnvironmentCrn());
        assertEquals(COMPLETED, result.getStatus());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    public void testSynchronizeStackFailsPartial() throws FreeIpaClientException, TimeoutException {
        UmsUsersState umsUsersState = mock(UmsUsersState.class);
        when(umsUsersState.getRequestedWorkloadUsernames()).thenReturn(ImmutableSet.of("user1", "user2"));
        UserSyncOptions options = UserSyncOptions.newBuilder()
                .fullSync(false)
                .fmsToFreeIpaBatchCallEnabled(false)
                .workloadCredentialsUpdateType(WorkloadCredentialsUpdateType.FORCE_UPDATE)
                .build();
        UsersState usersState = mock(UsersState.class);
        when(usersState.getUsers()).thenReturn(ImmutableSet.of());
        when(usersState.getGroups()).thenReturn(ImmutableSet.of());
        when(freeIpaUsersStateProvider.getFilteredFreeIpaState(FREE_IPA_CLIENT, Set.of("user1", "user2"))).thenReturn(usersState);
        UsersStateDifference usersStateDifference = mock(UsersStateDifference.class);
        when(userStateDifferenceCalculator.fromUmsAndIpaUsersStates(eq(umsUsersState), eq(usersState), eq(options), any()))
                .thenReturn(usersStateDifference);
        doAnswer(invocation -> {
            Multimap<String, String> warnings = invocation.getArgument(2, Multimap.class);
            warnings.put(ENV_CRN, "failed");
            return null;
        })
                .when(stateApplier).applyDifference(eq(umsUsersState), eq(ENV_CRN), any(), eq(usersStateDifference), eq(options), eq(FREE_IPA_CLIENT),
                        eq(STACK_ID));

        SyncStatusDetail result = underTest.synchronizeStack(STACK, umsUsersState, options, OPERATION_ID);

        verify(freeIpaUsersStateProvider, never()).getUsersState(any(), anyBoolean());
        verify(stateApplier).applyDifference(eq(umsUsersState), eq(ENV_CRN), any(), eq(usersStateDifference), eq(options), eq(FREE_IPA_CLIENT), eq(STACK_ID));
        verifyNoMoreInteractions(stateApplier);
        verifyNoInteractions(cloudIdentitySyncService);
        verifyNoInteractions(sudoRuleService);
        verify(authDistributorService, never()).updateAuthViewForEnvironment(eq(ENV_CRN), any(), eq(ACCOUNT), eq(OPERATION_ID));
        assertEquals(ENV_CRN, result.getEnvironmentCrn());
        assertEquals(FAILED, result.getStatus());
        assertFalse(result.getWarnings().isEmpty());
    }

    @Test
    public void testSynchronizeStackFailsToGetClient() throws FreeIpaClientException {
        UmsUsersState umsUsersState = mock(UmsUsersState.class);
        UserSyncOptions options = UserSyncOptions.newBuilder()
                .fullSync(false)
                .fmsToFreeIpaBatchCallEnabled(false)
                .workloadCredentialsUpdateType(WorkloadCredentialsUpdateType.FORCE_UPDATE)
                .build();
        when(freeIpaClientFactory.getFreeIpaClientForStackId(STACK_ID)).thenThrow(new FreeIpaClientException("potty"));

        SyncStatusDetail result = underTest.synchronizeStack(STACK, umsUsersState, options, OPERATION_ID);

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
    public void testSynchronizeStackForDeleteUser() throws FreeIpaClientException, TimeoutException {
        UsersState usersState = mock(UsersState.class);
        when(usersState.getUsers()).thenReturn(ImmutableSet.of(new FmsUser().withName("deleteMe")));
        when(usersState.getGroups()).thenReturn(ImmutableSet.of());
        when(freeIpaUsersStateProvider.getFilteredFreeIpaState(FREE_IPA_CLIENT, Set.of("deleteMe"))).thenReturn(usersState);

        SyncStatusDetail result = underTest.synchronizeStackForDeleteUser(STACK, "deleteMe");

        verify(stateApplier).applyUserDeleteToIpa(eq(ENV_CRN), eq(FREE_IPA_CLIENT), eq("deleteMe"), any(), eq(false));
        assertEquals(ENV_CRN, result.getEnvironmentCrn());
        assertEquals(COMPLETED, result.getStatus());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    public void testSynchronizeStackForDeleteUserFailure() throws FreeIpaClientException {
        when(freeIpaClientFactory.getFreeIpaClientForStackId(STACK_ID)).thenThrow(new FreeIpaClientException("totty"));

        SyncStatusDetail result = underTest.synchronizeStackForDeleteUser(STACK, "deleteMe");

        verifyNoInteractions(stateApplier);
        assertEquals(ENV_CRN, result.getEnvironmentCrn());
        assertEquals(FAILED, result.getStatus());
        assertTrue(result.getWarnings().isEmpty());
    }
}