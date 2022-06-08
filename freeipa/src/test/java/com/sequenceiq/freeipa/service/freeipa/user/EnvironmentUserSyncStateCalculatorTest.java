package com.sequenceiq.freeipa.service.freeipa.user;

import static com.sequenceiq.freeipa.service.freeipa.user.UserSyncTestUtils.ACCOUNT_ID;
import static com.sequenceiq.freeipa.service.freeipa.user.UserSyncTestUtils.ENVIRONMENT_CRN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
import com.sequenceiq.freeipa.entity.UserSyncStatus;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsEventGenerationIds;
import com.sequenceiq.freeipa.service.freeipa.user.ums.UmsEventGenerationIdsProvider;

@ExtendWith(MockitoExtension.class)
class EnvironmentUserSyncStateCalculatorTest {

    @Mock
    UmsEventGenerationIdsProvider umsEventGenerationIdsProvider;

    @Mock
    EventGenerationIdsChecker eventGenerationIdsChecker;

    @InjectMocks
    EnvironmentUserSyncStateCalculator underTest;

    @Test
    void internalCalculateEnvironmentUserSyncStateNoStatus() {
        EnvironmentUserSyncState result = underTest.internalCalculateEnvironmentUserSyncState(ACCOUNT_ID, ENVIRONMENT_CRN, Optional.empty());

        assertEquals(UserSyncState.STALE, result.getState());
        assertNull(result.getLastUserSyncOperationId());
    }

    @Test
    void internalCalculateEnvironmentUserSyncStateNoLastSync() {
        UserSyncStatus userSyncStatus = new UserSyncStatus();

        EnvironmentUserSyncState result = underTest.internalCalculateEnvironmentUserSyncState(ACCOUNT_ID, ENVIRONMENT_CRN, Optional.of(userSyncStatus));

        assertEquals(UserSyncState.STALE, result.getState());
        assertNull(result.getLastUserSyncOperationId());
    }

    @Test
    void internalCalculateEnvironmentUserSyncStateLastSyncRunning() {
        UserSyncStatus userSyncStatus = new UserSyncStatus();
        Operation lastSync = new Operation();
        lastSync.setOperationId(UUID.randomUUID().toString());
        lastSync.setStatus(OperationState.RUNNING);
        userSyncStatus.setLastStartedFullSync(lastSync);

        EnvironmentUserSyncState result = underTest.internalCalculateEnvironmentUserSyncState(ACCOUNT_ID, ENVIRONMENT_CRN, Optional.of(userSyncStatus));

        assertEquals(UserSyncState.SYNC_IN_PROGRESS, result.getState());
        assertEquals(lastSync.getOperationId(), result.getLastUserSyncOperationId());
    }

    @Test
    void internalCalculateEnvironmentUserSyncStateLastSyncFailed() {
        UserSyncStatus userSyncStatus = new UserSyncStatus();
        Operation lastSync = new Operation();
        lastSync.setOperationId(UUID.randomUUID().toString());
        lastSync.setStatus(OperationState.FAILED);
        userSyncStatus.setLastStartedFullSync(lastSync);

        EnvironmentUserSyncState result = underTest.internalCalculateEnvironmentUserSyncState(ACCOUNT_ID, ENVIRONMENT_CRN, Optional.of(userSyncStatus));

        assertEquals(UserSyncState.SYNC_FAILED, result.getState());
        assertEquals(lastSync.getOperationId(), result.getLastUserSyncOperationId());
    }

    @Test
    void internalCalculateEnvironmentUserSyncStateLastSyncTimedout() {
        UserSyncStatus userSyncStatus = new UserSyncStatus();
        Operation lastSync = new Operation();
        lastSync.setOperationId(UUID.randomUUID().toString());
        lastSync.setStatus(OperationState.FAILED);
        userSyncStatus.setLastStartedFullSync(lastSync);

        EnvironmentUserSyncState result = underTest.internalCalculateEnvironmentUserSyncState(ACCOUNT_ID, ENVIRONMENT_CRN, Optional.of(userSyncStatus));

        assertEquals(UserSyncState.SYNC_FAILED, result.getState());
        assertEquals(lastSync.getOperationId(), result.getLastUserSyncOperationId());
    }

    @Test
    void internalCalculateEnvironmentUserSyncStateLastSyncRequested() {
        UserSyncStatus userSyncStatus = new UserSyncStatus();
        Operation lastSync = new Operation();
        lastSync.setOperationId(UUID.randomUUID().toString());
        lastSync.setStatus(OperationState.REQUESTED);
        userSyncStatus.setLastStartedFullSync(lastSync);

        assertThrows(IllegalStateException.class, () -> underTest.internalCalculateEnvironmentUserSyncState(ACCOUNT_ID, ENVIRONMENT_CRN,
                Optional.of(userSyncStatus)));
    }

    @Test
    void internalCalculateEnvironmentUserSyncStateLastSyncRejected() {
        UserSyncStatus userSyncStatus = new UserSyncStatus();
        Operation lastSync = new Operation();
        lastSync.setOperationId(UUID.randomUUID().toString());
        lastSync.setStatus(OperationState.REJECTED);
        userSyncStatus.setLastStartedFullSync(lastSync);

        assertThrows(IllegalStateException.class, () -> underTest.internalCalculateEnvironmentUserSyncState(ACCOUNT_ID, ENVIRONMENT_CRN,
                Optional.of(userSyncStatus)));
    }

    @Test
    void internalCalculateEnvironmentUserSyncStateLastSyncCompletedFailure() {
        UserSyncStatus userSyncStatus = new UserSyncStatus();
        Operation lastSync = new Operation();
        lastSync.setOperationId(UUID.randomUUID().toString());
        lastSync.setStatus(OperationState.COMPLETED);
        lastSync.setFailureList(List.of(new FailureDetails(ENVIRONMENT_CRN, "failure message")));
        userSyncStatus.setLastStartedFullSync(lastSync);

        EnvironmentUserSyncState result = underTest.internalCalculateEnvironmentUserSyncState(ACCOUNT_ID, ENVIRONMENT_CRN, Optional.of(userSyncStatus));

        assertEquals(UserSyncState.SYNC_FAILED, result.getState());
        assertEquals(lastSync.getOperationId(), result.getLastUserSyncOperationId());
    }

    @Test
    void internalCalculateEnvironmentUserSyncStateLastSyncCompletedSuccessNotInSync() {
        UserSyncStatus userSyncStatus = new UserSyncStatus();
        Operation lastSync = new Operation();
        lastSync.setOperationId(UUID.randomUUID().toString());
        lastSync.setStatus(OperationState.COMPLETED);
        lastSync.setSuccessList(List.of(new SuccessDetails(ENVIRONMENT_CRN)));
        userSyncStatus.setLastStartedFullSync(lastSync);

        UmsEventGenerationIds current = new UmsEventGenerationIds();
        when(umsEventGenerationIdsProvider.getEventGenerationIds(eq(ACCOUNT_ID))).thenReturn(current);

        when(eventGenerationIdsChecker.isInSync(userSyncStatus, current)).thenReturn(false);

        EnvironmentUserSyncState result = underTest.internalCalculateEnvironmentUserSyncState(ACCOUNT_ID, ENVIRONMENT_CRN, Optional.of(userSyncStatus));

        assertEquals(UserSyncState.STALE, result.getState());
        assertEquals(lastSync.getOperationId(), result.getLastUserSyncOperationId());
    }

    @Test
    void internalCalculateEnvironmentUserSyncStateLastSyncCompletedSuccessInSync() {
        UserSyncStatus userSyncStatus = new UserSyncStatus();
        Operation lastSync = new Operation();
        lastSync.setOperationId(UUID.randomUUID().toString());
        lastSync.setStatus(OperationState.COMPLETED);
        lastSync.setSuccessList(List.of(new SuccessDetails(ENVIRONMENT_CRN)));
        userSyncStatus.setLastStartedFullSync(lastSync);

        UmsEventGenerationIds current = new UmsEventGenerationIds();
        when(umsEventGenerationIdsProvider.getEventGenerationIds(eq(ACCOUNT_ID))).thenReturn(current);

        when(eventGenerationIdsChecker.isInSync(userSyncStatus, current)).thenReturn(true);

        EnvironmentUserSyncState result = underTest.internalCalculateEnvironmentUserSyncState(ACCOUNT_ID, ENVIRONMENT_CRN, Optional.of(userSyncStatus));

        assertEquals(UserSyncState.UP_TO_DATE, result.getState());
        assertEquals(lastSync.getOperationId(), result.getLastUserSyncOperationId());
    }
}