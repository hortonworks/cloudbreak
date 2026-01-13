package com.sequenceiq.freeipa.service.freeipa.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.CommonPermissionCheckingUtils;
import com.sequenceiq.authorization.service.CustomCheckUtil;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.WorkloadCredentialsUpdateType;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.UserSyncStatus;
import com.sequenceiq.freeipa.entity.projection.StackUserSyncView;
import com.sequenceiq.freeipa.service.freeipa.user.model.UserSyncOptions;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
public class UserSyncServiceTest {
    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:"
            + ACCOUNT_ID + ":environment:" + UUID.randomUUID();

    private static final String ENV_CRN_2 = "crn:cdp:environments:us-west-1:"
            + ACCOUNT_ID + ":environment:" + UUID.randomUUID();

    private static final String ACTOR_CRN = "crn:cdp:iam:us-west-1:"
            + ACCOUNT_ID + ":user:" + UUID.randomUUID();

    private static final String INTERNAL_ACTOR = "crn:cdp:iam:us-west-1:altus:user:__internal__actor__";

    private static final Long TIMEOUT = 6L;

    private static final long STACK_ID = 2L;

    @Mock
    private StackService stackService;

    @Mock
    private OperationService operationService;

    @Mock
    private UserSyncStatusService userSyncStatusService;

    @Mock
    private UserSyncRequestValidator userSyncRequestValidator;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private ExecutorService asyncTaskExecutor;

    @Mock
    private UserSyncForEnvService userSyncForEnvService;

    @Mock
    private CustomCheckUtil customCheckUtil;

    @Mock
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Mock
    private TimeoutTaskScheduler timeoutTaskScheduler;

    @InjectMocks
    private UserSyncService underTest;

    @Test
    public void testSyncUsers() {
        StackUserSyncView stack = mock(StackUserSyncView.class);
        when(stack.environmentCrn()).thenReturn(ENV_CRN);
        when(stack.id()).thenReturn(STACK_ID);
        when(stackService.getAllUserSyncViewByEnvironmentCrnOrChildEnvironmentCrnAndAccountId(Set.of(), ACCOUNT_ID)).thenReturn(List.of(stack));
        Operation operation = createRunningOperation();
        when(operationService.startOperation(anyString(), any(OperationType.class), anyCollection(), anyCollection()))
                .thenReturn(operation);
        doAnswer(inv -> {
            Runnable runnable = inv.getArgument(2, Runnable.class);
            runnable.run();
            return null;
        }).when(operationService).tryWithOperationCleanup(eq(operation.getOperationId()), eq(ACCOUNT_ID), any(Runnable.class));
        UserSyncStatus userSyncStatus = new UserSyncStatus();
        when(userSyncStatusService.getOrCreateForStack(STACK_ID)).thenReturn(userSyncStatus);
        when(entitlementService.usersyncCredentialsUpdateOptimizationEnabled(ACCOUNT_ID)).thenReturn(Boolean.TRUE);
        when(entitlementService.isFmsToFreeipaBatchCallEnabled(ACCOUNT_ID)).thenReturn(Boolean.TRUE);
        doAnswer(inv -> {
            Runnable runnable = inv.getArgument(0, Runnable.class);
            assertEquals(operation.getOperationId(), MDCBuilder.getMdcContextMap().get(LoggerContextKey.OPERATION_ID.toString()));
            assertEquals(INTERNAL_ACTOR, ThreadBasedUserCrnProvider.getUserCrn());
            runnable.run();
            return mock(Future.class);
        }).when(asyncTaskExecutor).submit(any(Runnable.class));


        Operation result = underTest.synchronizeUsers(ACCOUNT_ID, ACTOR_CRN, Set.of(), Set.of(), Set.of(), WorkloadCredentialsUpdateType.UPDATE_IF_CHANGED);

        assertEquals(operation, result);
        ArgumentCaptor<UserSyncRequestFilter> requestFilterCaptor = ArgumentCaptor.forClass(UserSyncRequestFilter.class);
        verify(userSyncRequestValidator).validateParameters(eq(ACCOUNT_ID), eq(ACTOR_CRN), eq(Set.of()), requestFilterCaptor.capture());
        UserSyncRequestFilter requestFilter = requestFilterCaptor.getValue();
        assertTrue(requestFilter.getUserCrnFilter().isEmpty());
        assertTrue(requestFilter.getMachineUserCrnFilter().isEmpty());
        assertTrue(requestFilter.getDeletedWorkloadUser().isEmpty());
        assertEquals(operation, userSyncStatus.getLastStartedFullSync());
        verify(userSyncStatusService).save(userSyncStatus);
        ArgumentCaptor<UserSyncOptions> syncOptionsCaptor = ArgumentCaptor.forClass(UserSyncOptions.class);
        verify(userSyncForEnvService)
                .synchronizeUsers(eq(operation.getOperationId()), eq(ACCOUNT_ID), eq(List.of(stack)), eq(requestFilter), syncOptionsCaptor.capture(), anyLong());
        UserSyncOptions userSyncOptions = syncOptionsCaptor.getValue();
        assertTrue(userSyncOptions.isFullSync());
        assertTrue(userSyncOptions.isCredentialsUpdateOptimizationEnabled());
        assertTrue(userSyncOptions.isFmsToFreeIpaBatchCallEnabled());
    }

    @Test
    public void testSyncUsersWithFilterAndMultipleStack() {
        StackUserSyncView stack = mock(StackUserSyncView.class);
        StackUserSyncView stack2 = mock(StackUserSyncView.class);
        when(stack.environmentCrn()).thenReturn(ENV_CRN);
        when(stack2.environmentCrn()).thenReturn(ENV_CRN_2);
        when(stackService.getAllUserSyncViewByEnvironmentCrnOrChildEnvironmentCrnAndAccountId(Set.of(ENV_CRN, ENV_CRN_2), ACCOUNT_ID))
                .thenReturn(List.of(stack, stack2));
        Operation operation = createRunningOperation();
        when(operationService.startOperation(anyString(), any(OperationType.class), anyCollection(), anyCollection()))
                .thenReturn(operation);
        doAnswer(inv -> {
            Runnable runnable = inv.getArgument(2, Runnable.class);
            runnable.run();
            return null;
        }).when(operationService).tryWithOperationCleanup(eq(operation.getOperationId()), eq(ACCOUNT_ID), any(Runnable.class));
        when(entitlementService.usersyncCredentialsUpdateOptimizationEnabled(ACCOUNT_ID)).thenReturn(Boolean.TRUE);
        when(entitlementService.isFmsToFreeipaBatchCallEnabled(ACCOUNT_ID)).thenReturn(Boolean.TRUE);
        doAnswer(inv -> {
            Runnable runnable = inv.getArgument(0, Runnable.class);
            assertEquals(operation.getOperationId(), MDCBuilder.getMdcContextMap().get(LoggerContextKey.OPERATION_ID.toString()));
            assertEquals(INTERNAL_ACTOR, ThreadBasedUserCrnProvider.getUserCrn());
            runnable.run();
            return mock(Future.class);
        }).when(asyncTaskExecutor).submit(any(Runnable.class));


        Operation result = underTest.synchronizeUsers(ACCOUNT_ID, ACTOR_CRN, Set.of(ENV_CRN, ENV_CRN_2), Set.of("userCrn"), Set.of("machineUserCrn"),
                WorkloadCredentialsUpdateType.UPDATE_IF_CHANGED);

        assertEquals(operation, result);
        ArgumentCaptor<UserSyncRequestFilter> requestFilterCaptor = ArgumentCaptor.forClass(UserSyncRequestFilter.class);
        verify(userSyncRequestValidator).validateParameters(eq(ACCOUNT_ID), eq(ACTOR_CRN), eq(Set.of(ENV_CRN, ENV_CRN_2)), requestFilterCaptor.capture());
        UserSyncRequestFilter requestFilter = requestFilterCaptor.getValue();
        assertEquals(requestFilter.getUserCrnFilter(), Set.of("userCrn"));
        assertEquals(requestFilter.getMachineUserCrnFilter(), Set.of("machineUserCrn"));
        assertTrue(requestFilter.getDeletedWorkloadUser().isEmpty());
        verifyNoInteractions(userSyncStatusService);
        ArgumentCaptor<UserSyncOptions> syncOptionsCaptor = ArgumentCaptor.forClass(UserSyncOptions.class);
        verify(userSyncForEnvService)
                .synchronizeUsers(eq(operation.getOperationId()), eq(ACCOUNT_ID), eq(List.of(stack, stack2)), eq(requestFilter), syncOptionsCaptor.capture(),
                        anyLong());
        UserSyncOptions userSyncOptions = syncOptionsCaptor.getValue();
        assertFalse(userSyncOptions.isFullSync());
        assertTrue(userSyncOptions.isCredentialsUpdateOptimizationEnabled());
        assertTrue(userSyncOptions.isFmsToFreeIpaBatchCallEnabled());
    }

    @Test
    public void testSyncUsersWithCustomPermissionCheck() {
        StackUserSyncView stack = mock(StackUserSyncView.class);
        when(stack.environmentCrn()).thenReturn(ENV_CRN);
        when(stack.id()).thenReturn(STACK_ID);
        when(stackService.getAllUserSyncViewByEnvironmentCrnOrChildEnvironmentCrnAndAccountId(Set.of(), ACCOUNT_ID)).thenReturn(List.of(stack));
        Operation operation = createRunningOperation();
        when(operationService.startOperation(anyString(), any(OperationType.class), anyCollection(), anyCollection()))
                .thenReturn(operation);
        doAnswer(inv -> {
            Runnable runnable = inv.getArgument(2, Runnable.class);
            runnable.run();
            return null;
        }).when(operationService).tryWithOperationCleanup(eq(operation.getOperationId()), eq(ACCOUNT_ID), any(Runnable.class));
        UserSyncStatus userSyncStatus = new UserSyncStatus();
        when(userSyncStatusService.getOrCreateForStack(STACK_ID)).thenReturn(userSyncStatus);
        when(entitlementService.usersyncCredentialsUpdateOptimizationEnabled(ACCOUNT_ID)).thenReturn(Boolean.TRUE);
        when(entitlementService.isFmsToFreeipaBatchCallEnabled(ACCOUNT_ID)).thenReturn(Boolean.TRUE);
        doAnswer(inv -> {
            Runnable runnable = inv.getArgument(0, Runnable.class);
            assertEquals(operation.getOperationId(), MDCBuilder.getMdcContextMap().get(LoggerContextKey.OPERATION_ID.toString()));
            assertEquals(INTERNAL_ACTOR, ThreadBasedUserCrnProvider.getUserCrn());
            runnable.run();
            return mock(Future.class);
        }).when(asyncTaskExecutor).submit(any(Runnable.class));
        UserSyncRequestFilter userSyncFilter = new UserSyncRequestFilter(Set.of(), Set.of(), Optional.empty());
        doAnswer(inv -> {
            Runnable runnable = inv.getArgument(1, Runnable.class);
            runnable.run();
            return null;
        }).when(customCheckUtil).run(eq(ACTOR_CRN), any(Runnable.class));

        Operation result = underTest.synchronizeUsersWithCustomPermissionCheck(ACCOUNT_ID, ACTOR_CRN, Set.of(), userSyncFilter,
                WorkloadCredentialsUpdateType.UPDATE_IF_CHANGED, AuthorizationResourceAction.DESCRIBE_ENVIRONMENT);

        assertEquals(operation, result);
        ArgumentCaptor<UserSyncRequestFilter> requestFilterCaptor = ArgumentCaptor.forClass(UserSyncRequestFilter.class);
        verify(userSyncRequestValidator).validateParameters(eq(ACCOUNT_ID), eq(ACTOR_CRN), eq(Set.of()), requestFilterCaptor.capture());
        UserSyncRequestFilter requestFilter = requestFilterCaptor.getValue();
        assertTrue(requestFilter.getUserCrnFilter().isEmpty());
        assertTrue(requestFilter.getMachineUserCrnFilter().isEmpty());
        assertTrue(requestFilter.getDeletedWorkloadUser().isEmpty());
        assertEquals(operation, userSyncStatus.getLastStartedFullSync());
        verify(userSyncStatusService).save(userSyncStatus);
        ArgumentCaptor<UserSyncOptions> syncOptionsCaptor = ArgumentCaptor.forClass(UserSyncOptions.class);
        verify(userSyncForEnvService)
                .synchronizeUsers(eq(operation.getOperationId()), eq(ACCOUNT_ID), eq(List.of(stack)), eq(requestFilter), syncOptionsCaptor.capture(), anyLong());
        UserSyncOptions userSyncOptions = syncOptionsCaptor.getValue();
        assertTrue(userSyncOptions.isFullSync());
        assertTrue(userSyncOptions.isCredentialsUpdateOptimizationEnabled());
        assertTrue(userSyncOptions.isFmsToFreeIpaBatchCallEnabled());
        verify(commonPermissionCheckingUtils).checkPermissionForUserOnResources(AuthorizationResourceAction.DESCRIBE_ENVIRONMENT, ACTOR_CRN, List.of(ENV_CRN));
    }

    @Test
    public void testSyncUsersWithTimeoutCheckTaskFinished() {
        StackUserSyncView stack = mock(StackUserSyncView.class);
        when(stack.environmentCrn()).thenReturn(ENV_CRN);
        when(stack.id()).thenReturn(STACK_ID);
        when(stackService.getAllUserSyncViewByEnvironmentCrnOrChildEnvironmentCrnAndAccountId(Set.of(), ACCOUNT_ID)).thenReturn(List.of(stack));
        Operation operation = createRunningOperation();
        when(operationService.startOperation(anyString(), any(OperationType.class), anyCollection(), anyCollection()))
                .thenReturn(operation);
        doAnswer(inv -> {
            Runnable runnable = inv.getArgument(2, Runnable.class);
            runnable.run();
            return null;
        }).when(operationService).tryWithOperationCleanup(eq(operation.getOperationId()), eq(ACCOUNT_ID), any(Runnable.class));
        UserSyncStatus userSyncStatus = new UserSyncStatus();
        when(userSyncStatusService.getOrCreateForStack(STACK_ID)).thenReturn(userSyncStatus);
        when(entitlementService.usersyncCredentialsUpdateOptimizationEnabled(ACCOUNT_ID)).thenReturn(Boolean.TRUE);
        when(entitlementService.isFmsToFreeipaBatchCallEnabled(ACCOUNT_ID)).thenReturn(Boolean.TRUE);
        Future<?> usersyncTask = mock(Future.class);
        doAnswer(inv -> {
            Runnable runnable = inv.getArgument(0, Runnable.class);
            assertEquals(operation.getOperationId(), MDCBuilder.getMdcContextMap().get(LoggerContextKey.OPERATION_ID.toString()));
            assertEquals(INTERNAL_ACTOR, ThreadBasedUserCrnProvider.getUserCrn());
            runnable.run();
            return usersyncTask;
        }).when(asyncTaskExecutor).submit(any(Runnable.class));

        Operation result = underTest.synchronizeUsers(ACCOUNT_ID, ACTOR_CRN, Set.of(), Set.of(), Set.of(), WorkloadCredentialsUpdateType.UPDATE_IF_CHANGED);

        assertEquals(operation, result);
        ArgumentCaptor<UserSyncRequestFilter> requestFilterCaptor = ArgumentCaptor.forClass(UserSyncRequestFilter.class);
        verify(userSyncRequestValidator).validateParameters(eq(ACCOUNT_ID), eq(ACTOR_CRN), eq(Set.of()), requestFilterCaptor.capture());
        UserSyncRequestFilter requestFilter = requestFilterCaptor.getValue();
        assertTrue(requestFilter.getUserCrnFilter().isEmpty());
        assertTrue(requestFilter.getMachineUserCrnFilter().isEmpty());
        assertTrue(requestFilter.getDeletedWorkloadUser().isEmpty());
        assertEquals(operation, userSyncStatus.getLastStartedFullSync());
        verify(userSyncStatusService).save(userSyncStatus);
        ArgumentCaptor<UserSyncOptions> syncOptionsCaptor = ArgumentCaptor.forClass(UserSyncOptions.class);
        verify(userSyncForEnvService)
                .synchronizeUsers(eq(operation.getOperationId()), eq(ACCOUNT_ID), eq(List.of(stack)), eq(requestFilter), syncOptionsCaptor.capture(), anyLong());
        UserSyncOptions userSyncOptions = syncOptionsCaptor.getValue();
        assertTrue(userSyncOptions.isFullSync());
        assertTrue(userSyncOptions.isCredentialsUpdateOptimizationEnabled());
        assertTrue(userSyncOptions.isFmsToFreeIpaBatchCallEnabled());
        verify(operationService, never()).timeout(anyString(), anyString());
    }

    @Test
    public void testSyncUsersWithTimeoutCheckTaskRunnable() {
        StackUserSyncView stack = mock(StackUserSyncView.class);
        when(stack.environmentCrn()).thenReturn(ENV_CRN);
        when(stack.id()).thenReturn(STACK_ID);
        when(stackService.getAllUserSyncViewByEnvironmentCrnOrChildEnvironmentCrnAndAccountId(Set.of(), ACCOUNT_ID)).thenReturn(List.of(stack));
        Operation operation = createRunningOperation();
        when(operationService.startOperation(anyString(), any(OperationType.class), anyCollection(), anyCollection()))
                .thenReturn(operation);
        doAnswer(inv -> {
            Runnable runnable = inv.getArgument(2, Runnable.class);
            runnable.run();
            return null;
        }).when(operationService).tryWithOperationCleanup(eq(operation.getOperationId()), eq(ACCOUNT_ID), any(Runnable.class));
        UserSyncStatus userSyncStatus = new UserSyncStatus();
        when(userSyncStatusService.getOrCreateForStack(STACK_ID)).thenReturn(userSyncStatus);
        when(entitlementService.usersyncCredentialsUpdateOptimizationEnabled(ACCOUNT_ID)).thenReturn(Boolean.TRUE);
        when(entitlementService.isFmsToFreeipaBatchCallEnabled(ACCOUNT_ID)).thenReturn(Boolean.TRUE);
        Future<?> usersyncTask = mock(Future.class);
        doAnswer(inv -> {
            Runnable runnable = inv.getArgument(0, Runnable.class);
            assertEquals(operation.getOperationId(), MDCBuilder.getMdcContextMap().get(LoggerContextKey.OPERATION_ID.toString()));
            assertEquals(INTERNAL_ACTOR, ThreadBasedUserCrnProvider.getUserCrn());
            runnable.run();
            return usersyncTask;
        }).when(asyncTaskExecutor).submit(any(Runnable.class));
        ReflectionTestUtils.setField(underTest, "operationTimeout", TIMEOUT);

        Operation result = underTest.synchronizeUsers(ACCOUNT_ID, ACTOR_CRN, Set.of(), Set.of(), Set.of(), WorkloadCredentialsUpdateType.UPDATE_IF_CHANGED);

        assertEquals(operation, result);
        ArgumentCaptor<UserSyncRequestFilter> requestFilterCaptor = ArgumentCaptor.forClass(UserSyncRequestFilter.class);
        verify(userSyncRequestValidator).validateParameters(eq(ACCOUNT_ID), eq(ACTOR_CRN), eq(Set.of()), requestFilterCaptor.capture());
        UserSyncRequestFilter requestFilter = requestFilterCaptor.getValue();
        assertTrue(requestFilter.getUserCrnFilter().isEmpty());
        assertTrue(requestFilter.getMachineUserCrnFilter().isEmpty());
        assertTrue(requestFilter.getDeletedWorkloadUser().isEmpty());
        assertEquals(operation, userSyncStatus.getLastStartedFullSync());
        verify(userSyncStatusService).save(userSyncStatus);
        ArgumentCaptor<UserSyncOptions> syncOptionsCaptor = ArgumentCaptor.forClass(UserSyncOptions.class);
        verify(userSyncForEnvService)
                .synchronizeUsers(eq(operation.getOperationId()), eq(ACCOUNT_ID), eq(List.of(stack)), eq(requestFilter), syncOptionsCaptor.capture(), anyLong());
        UserSyncOptions userSyncOptions = syncOptionsCaptor.getValue();
        assertTrue(userSyncOptions.isFullSync());
        assertTrue(userSyncOptions.isCredentialsUpdateOptimizationEnabled());
        assertTrue(userSyncOptions.isFmsToFreeIpaBatchCallEnabled());
        verify(timeoutTaskScheduler).scheduleTimeoutTask(operation.getOperationId(), ACCOUNT_ID, usersyncTask, TIMEOUT);
    }

    private Operation createRunningOperation() {
        Operation operation = new Operation();
        operation.setOperationId(UUID.randomUUID().toString());
        operation.setAccountId(ACCOUNT_ID);
        operation.setStatus(OperationState.RUNNING);
        operation.setOperationType(OperationType.USER_SYNC);
        return operation;
    }
}