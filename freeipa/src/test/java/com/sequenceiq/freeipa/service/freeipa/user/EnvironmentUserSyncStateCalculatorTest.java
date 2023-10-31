package com.sequenceiq.freeipa.service.freeipa.user;

import static com.sequenceiq.freeipa.service.freeipa.user.UserSyncTestUtils.ACCOUNT_ID;
import static com.sequenceiq.freeipa.service.freeipa.user.UserSyncTestUtils.ENVIRONMENT_CRN;
import static com.sequenceiq.freeipa.service.freeipa.user.UserSyncTestUtils.ENV_CRN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.api.v1.freeipa.user.model.EnvironmentUserSyncState;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.UserSyncState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.UserSyncStatus;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsEventGenerationIds;
import com.sequenceiq.freeipa.service.freeipa.user.ums.UmsEventGenerationIdsProvider;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class EnvironmentUserSyncStateCalculatorTest {

    @Mock
    private UmsEventGenerationIdsProvider umsEventGenerationIdsProvider;

    @Mock
    private EventGenerationIdsChecker eventGenerationIdsChecker;

    @Mock
    private UserSyncStatusService userSyncStatusService;

    @Mock
    private StackService stackService;

    private Stack stack = mock(Stack.class);

    @InjectMocks
    private EnvironmentUserSyncStateCalculator underTest;

    @BeforeEach
    void init() {
        when(stackService.getByEnvironmentCrnAndAccountId(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(stack.getAccountId()).thenReturn(ACCOUNT_ID);
    }

    @Test
    void calculateEnvironmentUserSyncStateNoStatus() {
        when(userSyncStatusService.findByStack(stack)).thenReturn(Optional.empty());

        EnvironmentUserSyncState result = underTest.calculateEnvironmentUserSyncState(ACCOUNT_ID, ENV_CRN);

        assertEquals(UserSyncState.STALE, result.getState());
        assertNull(result.getLastUserSyncOperationId());
    }

    @Test
    void calculateEnvironmentUserSyncStateNoLastSync() {
        UserSyncStatus userSyncStatus = new UserSyncStatus();
        when(userSyncStatusService.findByStack(stack)).thenReturn(Optional.of(userSyncStatus));

        EnvironmentUserSyncState result = underTest.calculateEnvironmentUserSyncState(ACCOUNT_ID, ENV_CRN);

        assertEquals(UserSyncState.STALE, result.getState());
        assertNull(result.getLastUserSyncOperationId());
    }

    @Test
    void calculateEnvironmentUserSyncStateLastSyncRunning() {
        UserSyncStatus userSyncStatus = new UserSyncStatus();
        Operation lastSync = new Operation();
        lastSync.setOperationId(UUID.randomUUID().toString());
        lastSync.setStatus(OperationState.RUNNING);
        userSyncStatus.setLastStartedFullSync(lastSync);
        when(userSyncStatusService.findByStack(stack)).thenReturn(Optional.of(userSyncStatus));

        EnvironmentUserSyncState result = underTest.calculateEnvironmentUserSyncState(ACCOUNT_ID, ENV_CRN);

        assertEquals(UserSyncState.SYNC_IN_PROGRESS, result.getState());
        assertEquals(lastSync.getOperationId(), result.getLastUserSyncOperationId());
    }

    @Test
    void calculateEnvironmentUserSyncStateLastSyncFailed() {
        UserSyncStatus userSyncStatus = new UserSyncStatus();
        Operation lastSync = new Operation();
        lastSync.setOperationId(UUID.randomUUID().toString());
        lastSync.setStatus(OperationState.FAILED);
        userSyncStatus.setLastStartedFullSync(lastSync);
        when(userSyncStatusService.findByStack(stack)).thenReturn(Optional.of(userSyncStatus));

        EnvironmentUserSyncState result = underTest.calculateEnvironmentUserSyncState(ACCOUNT_ID, ENV_CRN);

        assertEquals(UserSyncState.SYNC_FAILED, result.getState());
        assertEquals(lastSync.getOperationId(), result.getLastUserSyncOperationId());
    }

    @Test
    void calculateEnvironmentUserSyncStateLastSyncTimedout() {
        UserSyncStatus userSyncStatus = new UserSyncStatus();
        Operation lastSync = new Operation();
        lastSync.setOperationId(UUID.randomUUID().toString());
        lastSync.setStatus(OperationState.FAILED);
        userSyncStatus.setLastStartedFullSync(lastSync);
        when(userSyncStatusService.findByStack(stack)).thenReturn(Optional.of(userSyncStatus));

        EnvironmentUserSyncState result = underTest.calculateEnvironmentUserSyncState(ACCOUNT_ID, ENV_CRN);

        assertEquals(UserSyncState.SYNC_FAILED, result.getState());
        assertEquals(lastSync.getOperationId(), result.getLastUserSyncOperationId());
    }

    @Test
    void calculateEnvironmentUserSyncStateLastSyncRequested() {
        UserSyncStatus userSyncStatus = new UserSyncStatus();
        Operation lastSync = new Operation();
        lastSync.setOperationId(UUID.randomUUID().toString());
        lastSync.setStatus(OperationState.REQUESTED);
        userSyncStatus.setLastStartedFullSync(lastSync);
        when(userSyncStatusService.findByStack(stack)).thenReturn(Optional.of(userSyncStatus));

        assertThrows(IllegalStateException.class, () -> underTest.calculateEnvironmentUserSyncState(ACCOUNT_ID, ENV_CRN));
    }

    @Test
    void calculateEnvironmentUserSyncStateLastSyncRejected() {
        UserSyncStatus userSyncStatus = new UserSyncStatus();
        Operation lastSync = new Operation();
        lastSync.setOperationId(UUID.randomUUID().toString());
        lastSync.setStatus(OperationState.REJECTED);
        userSyncStatus.setLastStartedFullSync(lastSync);
        when(userSyncStatusService.findByStack(stack)).thenReturn(Optional.of(userSyncStatus));

        assertThrows(IllegalStateException.class, () -> underTest.calculateEnvironmentUserSyncState(ACCOUNT_ID, ENV_CRN));
    }

    @Test
    void calculateEnvironmentUserSyncStateLastSyncCompletedFailure() {
        UserSyncStatus userSyncStatus = new UserSyncStatus();
        Operation lastSync = new Operation();
        lastSync.setOperationId(UUID.randomUUID().toString());
        lastSync.setStatus(OperationState.COMPLETED);
        lastSync.setFailureList(List.of(new FailureDetails(ENVIRONMENT_CRN, "failure message")));
        userSyncStatus.setLastStartedFullSync(lastSync);
        when(userSyncStatusService.findByStack(stack)).thenReturn(Optional.of(userSyncStatus));

        EnvironmentUserSyncState result = underTest.calculateEnvironmentUserSyncState(ACCOUNT_ID, ENV_CRN);

        assertEquals(UserSyncState.SYNC_FAILED, result.getState());
        assertEquals(lastSync.getOperationId(), result.getLastUserSyncOperationId());
    }

    @Test
    void calculateEnvironmentUserSyncStateLastSyncCompletedSuccessNotInSync() {
        UserSyncStatus userSyncStatus = new UserSyncStatus();
        Operation lastSync = new Operation();
        lastSync.setOperationId(UUID.randomUUID().toString());
        lastSync.setStatus(OperationState.COMPLETED);
        lastSync.setSuccessList(List.of(new SuccessDetails(ENVIRONMENT_CRN)));
        userSyncStatus.setLastStartedFullSync(lastSync);
        when(userSyncStatusService.findByStack(stack)).thenReturn(Optional.of(userSyncStatus));

        UmsEventGenerationIds current = new UmsEventGenerationIds();
        when(umsEventGenerationIdsProvider.getEventGenerationIds(eq(ACCOUNT_ID))).thenReturn(current);

        when(eventGenerationIdsChecker.isInSync(userSyncStatus, current, stack)).thenReturn(false);

        EnvironmentUserSyncState result = underTest.calculateEnvironmentUserSyncState(ACCOUNT_ID, ENV_CRN);

        assertEquals(UserSyncState.STALE, result.getState());
        assertEquals(lastSync.getOperationId(), result.getLastUserSyncOperationId());
    }

    @Test
    void calculateEnvironmentUserSyncStateLastSyncCompletedSuccessInSync() {
        UserSyncStatus userSyncStatus = new UserSyncStatus();
        Operation lastSync = new Operation();
        lastSync.setOperationId(UUID.randomUUID().toString());
        lastSync.setStatus(OperationState.COMPLETED);
        lastSync.setSuccessList(List.of(new SuccessDetails(ENVIRONMENT_CRN)));
        userSyncStatus.setLastStartedFullSync(lastSync);
        when(userSyncStatusService.findByStack(stack)).thenReturn(Optional.of(userSyncStatus));

        UmsEventGenerationIds current = new UmsEventGenerationIds();
        when(umsEventGenerationIdsProvider.getEventGenerationIds(eq(ACCOUNT_ID))).thenReturn(current);

        when(eventGenerationIdsChecker.isInSync(userSyncStatus, current, stack)).thenReturn(true);

        EnvironmentUserSyncState result = underTest.calculateEnvironmentUserSyncState(ACCOUNT_ID, ENV_CRN);

        assertEquals(UserSyncState.UP_TO_DATE, result.getState());
        assertEquals(lastSync.getOperationId(), result.getLastUserSyncOperationId());
    }
}