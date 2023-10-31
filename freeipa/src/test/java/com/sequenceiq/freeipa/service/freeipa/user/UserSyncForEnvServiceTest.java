package com.sequenceiq.freeipa.service.freeipa.user;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.ImmutableMultimap;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.WorkloadCredentialsUpdateType;
import com.sequenceiq.freeipa.configuration.UsersyncConfig;
import com.sequenceiq.freeipa.entity.UserSyncStatus;
import com.sequenceiq.freeipa.entity.projection.StackUserSyncView;
import com.sequenceiq.freeipa.service.freeipa.user.model.SyncStatusDetail;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsEventGenerationIds;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UserSyncOptions;
import com.sequenceiq.freeipa.service.freeipa.user.ums.UmsEventGenerationIdsProvider;
import com.sequenceiq.freeipa.service.freeipa.user.ums.UmsUsersStateProviderDispatcher;
import com.sequenceiq.freeipa.service.operation.OperationService;

@ExtendWith({MockitoExtension.class})
class UserSyncForEnvServiceTest {

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String OPERATION_ID = "opId";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:"
            + ACCOUNT_ID + ":environment:" + UUID.randomUUID();

    private static final String ENV_CRN_2 = "crn:cdp:environments:us-west-1:"
            + ACCOUNT_ID + ":environment:" + UUID.randomUUID();

    @Mock
    private UmsEventGenerationIdsProvider umsEventGenerationIdsProvider;

    @Mock
    private UserSyncForStackService userSyncForStackService;

    @Mock
    private UmsUsersStateProviderDispatcher umsUsersStateProviderDispatcher;

    @Mock
    @Qualifier(UsersyncConfig.USERSYNC_EXTERNAL_TASK_EXECUTOR)
    private ExecutorService asyncTaskExecutor;

    @Mock
    private OperationService operationService;

    @Mock
    private UserSyncStatusService userSyncStatusService;

    @Mock
    private UmsVirtualGroupCreateService umsVirtualGroupCreateService;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private UserSyncForEnvService underTest;

    @Test
    public void testSyncUsers() {
        StackUserSyncView stack1 = mock(StackUserSyncView.class);
        when(stack1.environmentCrn()).thenReturn(ENV_CRN);
        when(stack1.id()).thenReturn(1L);
        StackUserSyncView stack2 = mock(StackUserSyncView.class);
        when(stack2.environmentCrn()).thenReturn(ENV_CRN_2);
        when(stack2.id()).thenReturn(2L);
        UserSyncRequestFilter userSyncFilter = new UserSyncRequestFilter(Set.of(), Set.of(), Optional.empty());
        UserSyncOptions options = createUserSyncOptions();
        doAnswer(inv -> {
            inv.getArgument(2, Runnable.class).run();
            return null;
        }).when(operationService).tryWithOperationCleanup(eq(OPERATION_ID), eq(ACCOUNT_ID), any(Runnable.class));
        UmsUsersState umsUsersState1 = mock(UmsUsersState.class);
        UmsUsersState umsUsersState2 = mock(UmsUsersState.class);
        when(umsUsersStateProviderDispatcher
                .getEnvToUmsUsersStateMap(eq(ACCOUNT_ID), eq(Set.of(ENV_CRN, ENV_CRN_2)), eq(Set.of()), eq(Set.of()), eq(options)))
                .thenReturn(Map.of(ENV_CRN, umsUsersState1, ENV_CRN_2, umsUsersState2));
        when(asyncTaskExecutor.submit(any(Callable.class))).thenAnswer(inv -> {
            SyncStatusDetail result = (SyncStatusDetail) inv.getArgument(0, Callable.class).call();
            Future future = mock(Future.class);
            when(future.get()).thenReturn(result);
            return future;
        });
        when(umsEventGenerationIdsProvider.getEventGenerationIds(eq(ACCOUNT_ID))).thenReturn(new UmsEventGenerationIds());
        when(userSyncForStackService.synchronizeStack(stack1, umsUsersState1, options, OPERATION_ID))
                .thenReturn(new SyncStatusDetail(ENV_CRN, SynchronizationStatus.COMPLETED, "", ImmutableMultimap.of()));
        when(userSyncForStackService.synchronizeStack(stack2, umsUsersState2, options, OPERATION_ID))
                .thenReturn(new SyncStatusDetail(ENV_CRN_2, SynchronizationStatus.COMPLETED, "", ImmutableMultimap.of()));
        when(userSyncStatusService.getOrCreateForStack(1L)).thenReturn(new UserSyncStatus());
        when(userSyncStatusService.getOrCreateForStack(2L)).thenReturn(new UserSyncStatus());

        underTest.synchronizeUsers(OPERATION_ID, ACCOUNT_ID, List.of(stack1, stack2), userSyncFilter, options, System.currentTimeMillis());

        verify(umsVirtualGroupCreateService).createVirtualGroups(ACCOUNT_ID, List.of(stack1, stack2));
        verify(userSyncStatusService, times(2)).save(any(UserSyncStatus.class));
        ArgumentCaptor<Collection> successCaptor = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<Collection> failureCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(operationService).completeOperation(eq(ACCOUNT_ID), eq(OPERATION_ID), successCaptor.capture(), failureCaptor.capture());
        assertTrue(failureCaptor.getValue().isEmpty());
        assertTrue(successCaptor.getValue().contains(new SuccessDetails(ENV_CRN)));
        assertTrue(successCaptor.getValue().contains(new SuccessDetails(ENV_CRN_2)));
    }

    @Test
    public void testSyncUsersFailures() {
        StackUserSyncView stack1 = mock(StackUserSyncView.class);
        when(stack1.environmentCrn()).thenReturn(ENV_CRN);
        StackUserSyncView stack2 = mock(StackUserSyncView.class);
        when(stack2.environmentCrn()).thenReturn(ENV_CRN_2);
        UserSyncRequestFilter userSyncFilter = new UserSyncRequestFilter(Set.of(), Set.of(), Optional.empty());
        UserSyncOptions options = createUserSyncOptions();
        doAnswer(inv -> {
            inv.getArgument(2, Runnable.class).run();
            return null;
        }).when(operationService).tryWithOperationCleanup(eq(OPERATION_ID), eq(ACCOUNT_ID), any(Runnable.class));
        UmsUsersState umsUsersState1 = mock(UmsUsersState.class);
        UmsUsersState umsUsersState2 = mock(UmsUsersState.class);
        when(umsUsersStateProviderDispatcher
                .getEnvToUmsUsersStateMap(eq(ACCOUNT_ID), eq(Set.of(ENV_CRN, ENV_CRN_2)), eq(Set.of()), eq(Set.of()), eq(options)))
                .thenReturn(Map.of(ENV_CRN, umsUsersState1, ENV_CRN_2, umsUsersState2));
        when(asyncTaskExecutor.submit(any(Callable.class))).thenAnswer(inv -> {
            SyncStatusDetail result = (SyncStatusDetail) inv.getArgument(0, Callable.class).call();
            Future future = mock(Future.class);
            when(future.get()).thenReturn(result);
            return future;
        });
        when(umsEventGenerationIdsProvider.getEventGenerationIds(eq(ACCOUNT_ID))).thenReturn(new UmsEventGenerationIds());
        when(userSyncForStackService.synchronizeStack(stack1, umsUsersState1, options, OPERATION_ID))
                .thenReturn(new SyncStatusDetail(ENV_CRN, SynchronizationStatus.FAILED, "fial1", ImmutableMultimap.of(ENV_CRN, "failed1")));
        when(userSyncForStackService.synchronizeStack(stack2, umsUsersState2, options, OPERATION_ID))
                .thenReturn(new SyncStatusDetail(ENV_CRN_2, SynchronizationStatus.REJECTED, "fial2", ImmutableMultimap.of(ENV_CRN_2, "failed2")));

        underTest.synchronizeUsers(OPERATION_ID, ACCOUNT_ID, List.of(stack1, stack2), userSyncFilter, options, System.currentTimeMillis());

        verifyNoInteractions(userSyncStatusService);
        ArgumentCaptor<Collection> successCaptor = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<Collection> failureCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(operationService).completeOperation(eq(ACCOUNT_ID), eq(OPERATION_ID), successCaptor.capture(), failureCaptor.capture());
        assertTrue(successCaptor.getValue().isEmpty());
        List<FailureDetails> failures = (List<FailureDetails>) failureCaptor.getValue();
        assertThat(failures, allOf(
                hasItem(allOf(
                        hasProperty("environment", is(ENV_CRN)),
                        hasProperty("message", is("fial1")),
                        hasProperty("additionalDetails", hasEntry(ENV_CRN, "failed1"))
                )),
                hasItem(allOf(
                        hasProperty("environment", is(ENV_CRN_2)),
                        hasProperty("message", is("Unexpected status: REJECTED")),
                        hasProperty("additionalDetails", hasEntry(ENV_CRN_2, "failed2"))
                ))
        ));
    }

    @Test
    public void testSyncUsersInterrupted() {
        StackUserSyncView stack1 = mock(StackUserSyncView.class);
        when(stack1.environmentCrn()).thenReturn(ENV_CRN);
        UserSyncRequestFilter userSyncFilter = new UserSyncRequestFilter(Set.of(), Set.of(), Optional.empty());
        UserSyncOptions options = createUserSyncOptions();
        doAnswer(inv -> {
            inv.getArgument(2, Runnable.class).run();
            return null;
        }).when(operationService).tryWithOperationCleanup(eq(OPERATION_ID), eq(ACCOUNT_ID), any(Runnable.class));
        UmsUsersState umsUsersState1 = mock(UmsUsersState.class);
        when(umsUsersStateProviderDispatcher.getEnvToUmsUsersStateMap(eq(ACCOUNT_ID), eq(Set.of(ENV_CRN)), eq(Set.of()), eq(Set.of()), eq(options)))
                .thenReturn(Map.of(ENV_CRN, umsUsersState1));
        when(asyncTaskExecutor.submit(any(Callable.class))).thenAnswer(inv -> {
            Future future = mock(Future.class);
            when(future.get()).thenThrow(new InterruptedException("interrupt"));
            return future;
        });
        when(umsEventGenerationIdsProvider.getEventGenerationIds(eq(ACCOUNT_ID))).thenReturn(new UmsEventGenerationIds());

        underTest.synchronizeUsers(OPERATION_ID, ACCOUNT_ID, List.of(stack1), userSyncFilter, options, System.currentTimeMillis());

        verifyNoInteractions(userSyncStatusService);
        ArgumentCaptor<Collection> successCaptor = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<Collection> failureCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(operationService).completeOperation(eq(ACCOUNT_ID), eq(OPERATION_ID), successCaptor.capture(), failureCaptor.capture());
        assertTrue(successCaptor.getValue().isEmpty());
        List<FailureDetails> failures = (List<FailureDetails>) failureCaptor.getValue();
        assertThat(failures, allOf(
                hasItem(allOf(
                        hasProperty("environment", is(ENV_CRN)),
                        hasProperty("message", is("interrupt")),
                        hasProperty("additionalDetails", anEmptyMap())
                ))
        ));
    }

    @Test
    public void testSyncUsersTimesOut() {
        ReflectionTestUtils.setField(underTest, "operationTimeout", 0L);
        StackUserSyncView stack1 = mock(StackUserSyncView.class);
        when(stack1.environmentCrn()).thenReturn(ENV_CRN);
        UserSyncRequestFilter userSyncFilter = new UserSyncRequestFilter(Set.of(), Set.of(), Optional.empty());
        UserSyncOptions options = createUserSyncOptions();
        doAnswer(inv -> {
            inv.getArgument(2, Runnable.class).run();
            return null;
        }).when(operationService).tryWithOperationCleanup(eq(OPERATION_ID), eq(ACCOUNT_ID), any(Runnable.class));
        UmsUsersState umsUsersState1 = mock(UmsUsersState.class);
        when(umsUsersStateProviderDispatcher.getEnvToUmsUsersStateMap(eq(ACCOUNT_ID), eq(Set.of(ENV_CRN)), eq(Set.of()), eq(Set.of()), eq(options)))
                .thenReturn(Map.of(ENV_CRN, umsUsersState1));
        Future<?> future = mock(Future.class);
        when(asyncTaskExecutor.submit(any(Callable.class))).thenAnswer(inv -> {
            when(future.get(0L, TimeUnit.MILLISECONDS)).thenThrow(new TimeoutException("timeout"));
            return future;
        });
        when(umsEventGenerationIdsProvider.getEventGenerationIds(eq(ACCOUNT_ID))).thenReturn(new UmsEventGenerationIds());
        when(entitlementService.isUserSyncThreadTimeoutEnabled(ACCOUNT_ID)).thenReturn(Boolean.TRUE);

        underTest.synchronizeUsers(OPERATION_ID, ACCOUNT_ID, List.of(stack1), userSyncFilter, options, System.currentTimeMillis());

        verifyNoInteractions(userSyncStatusService);
        ArgumentCaptor<Collection> successCaptor = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<Collection> failureCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(operationService).completeOperation(eq(ACCOUNT_ID), eq(OPERATION_ID), successCaptor.capture(), failureCaptor.capture());
        assertTrue(successCaptor.getValue().isEmpty());
        verify(future).cancel(true);
        List<FailureDetails> failures = (List<FailureDetails>) failureCaptor.getValue();
        assertThat(failures, allOf(
                hasItem(allOf(
                        hasProperty("environment", is(ENV_CRN)),
                        hasProperty("message", is("Timed out")),
                        hasProperty("additionalDetails", anEmptyMap())
                ))
        ));
    }

    @Test
    public void testSyncUsersDoesntTimeout() {
        ReflectionTestUtils.setField(underTest, "operationTimeout", 0L);
        StackUserSyncView stack1 = mock(StackUserSyncView.class);
        when(stack1.environmentCrn()).thenReturn(ENV_CRN);
        when(stack1.id()).thenReturn(1L);
        UserSyncRequestFilter userSyncFilter = new UserSyncRequestFilter(Set.of(), Set.of(), Optional.empty());
        UserSyncOptions options = createUserSyncOptions();
        doAnswer(inv -> {
            inv.getArgument(2, Runnable.class).run();
            return null;
        }).when(operationService).tryWithOperationCleanup(eq(OPERATION_ID), eq(ACCOUNT_ID), any(Runnable.class));
        UmsUsersState umsUsersState1 = mock(UmsUsersState.class);
        when(umsUsersStateProviderDispatcher.getEnvToUmsUsersStateMap(eq(ACCOUNT_ID), eq(Set.of(ENV_CRN)), eq(Set.of()), eq(Set.of()), eq(options)))
                .thenReturn(Map.of(ENV_CRN, umsUsersState1));
        when(userSyncForStackService.synchronizeStack(stack1, umsUsersState1, options, OPERATION_ID))
                .thenReturn(new SyncStatusDetail(ENV_CRN, SynchronizationStatus.COMPLETED, "", ImmutableMultimap.of()));
        Future<SyncStatusDetail> future = mock(Future.class);
        when(asyncTaskExecutor.submit(any(Callable.class))).thenAnswer(inv -> {
            SyncStatusDetail result = (SyncStatusDetail) inv.getArgument(0, Callable.class).call();
            when(future.get(anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(result);
            return future;
        });
        when(umsEventGenerationIdsProvider.getEventGenerationIds(eq(ACCOUNT_ID))).thenReturn(new UmsEventGenerationIds());
        when(userSyncStatusService.getOrCreateForStack(1L)).thenReturn(new UserSyncStatus());
        when(entitlementService.isUserSyncThreadTimeoutEnabled(ACCOUNT_ID)).thenReturn(Boolean.TRUE);

        underTest.synchronizeUsers(OPERATION_ID, ACCOUNT_ID, List.of(stack1), userSyncFilter, options, System.currentTimeMillis());

        ArgumentCaptor<Collection> successCaptor = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<Collection> failureCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(operationService).completeOperation(eq(ACCOUNT_ID), eq(OPERATION_ID), successCaptor.capture(), failureCaptor.capture());
        verify(future, never()).cancel(true);
        verify(future, never()).cancel(false);
        assertTrue(failureCaptor.getValue().isEmpty());
        assertTrue(successCaptor.getValue().contains(new SuccessDetails(ENV_CRN)));
    }

    @Test
    public void testSyncUserDelete() {
        StackUserSyncView stack1 = mock(StackUserSyncView.class);
        when(stack1.environmentCrn()).thenReturn(ENV_CRN);
        StackUserSyncView stack2 = mock(StackUserSyncView.class);
        when(stack2.environmentCrn()).thenReturn(ENV_CRN_2);
        UserSyncRequestFilter userSyncFilter = new UserSyncRequestFilter(Set.of(), Set.of(), Optional.of("deleteMe"));
        UserSyncOptions options = createUserSyncOptions();
        doAnswer(inv -> {
            inv.getArgument(2, Runnable.class).run();
            return null;
        }).when(operationService).tryWithOperationCleanup(eq(OPERATION_ID), eq(ACCOUNT_ID), any(Runnable.class));
        when(asyncTaskExecutor.submit(any(Callable.class))).thenAnswer(inv -> {
            SyncStatusDetail result = (SyncStatusDetail) inv.getArgument(0, Callable.class).call();
            Future future = mock(Future.class);
            when(future.get()).thenReturn(result);
            return future;
        });
        when(userSyncForStackService.synchronizeStackForDeleteUser(stack1, "deleteMe"))
                .thenReturn(new SyncStatusDetail(ENV_CRN, SynchronizationStatus.COMPLETED, "", ImmutableMultimap.of()));
        when(userSyncForStackService.synchronizeStackForDeleteUser(stack2, "deleteMe"))
                .thenReturn(new SyncStatusDetail(ENV_CRN_2, SynchronizationStatus.COMPLETED, "", ImmutableMultimap.of()));

        underTest.synchronizeUsers(OPERATION_ID, ACCOUNT_ID, List.of(stack1, stack2), userSyncFilter, options, System.currentTimeMillis());

        verifyNoInteractions(userSyncStatusService);
        ArgumentCaptor<Collection> successCaptor = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<Collection> failureCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(operationService).completeOperation(eq(ACCOUNT_ID), eq(OPERATION_ID), successCaptor.capture(), failureCaptor.capture());
        assertTrue(failureCaptor.getValue().isEmpty());
        assertTrue(successCaptor.getValue().contains(new SuccessDetails(ENV_CRN)));
        assertTrue(successCaptor.getValue().contains(new SuccessDetails(ENV_CRN_2)));
    }

    private UserSyncOptions createUserSyncOptions() {
        return UserSyncOptions.newBuilder()
                .fullSync(true)
                .fmsToFreeIpaBatchCallEnabled(true)
                .workloadCredentialsUpdateType(WorkloadCredentialsUpdateType.UPDATE_IF_CHANGED)
                .enforceGroupMembershipLimitEnabled(true)
                .largeGroupThreshold(500)
                .largeGroupLimit(750)
                .build();
    }
}