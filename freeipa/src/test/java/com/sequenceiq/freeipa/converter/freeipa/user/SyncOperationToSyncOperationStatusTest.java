package com.sequenceiq.freeipa.converter.freeipa.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationType;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;
import com.sequenceiq.freeipa.entity.SyncOperation;

class SyncOperationToSyncOperationStatusTest {

    private static final String OPERATION_ID = "operationId";

    private static final String ACCOUNT_ID = "accountId";

    private static final SyncOperationType SYNC_OPERATION_TYPE = SyncOperationType.SET_PASSWORD;

    private static final Long START_TIME = System.currentTimeMillis();

    private static final Long END_TIME = START_TIME + 1000L;

    private SyncOperationToSyncOperationStatus underTest = new SyncOperationToSyncOperationStatus();

    @Test
    void convertRunning() {
        SynchronizationStatus synchronizationStatus = SynchronizationStatus.RUNNING;

        SyncOperation syncOperation = createSyncOperation(synchronizationStatus);

        SyncOperationStatus actual = underTest.convert(syncOperation);

        assertEqualsDefaults(actual);
        assertEquals(synchronizationStatus, actual.getStatus());
        assertEquals(List.of(), actual.getSuccess());
        assertEquals(List.of(), actual.getFailure());
        assertNull(actual.getError());
    }

    @Test
    void convertCompleted() {
        SynchronizationStatus synchronizationStatus = SynchronizationStatus.COMPLETED;
        List<SuccessDetails> successDetails = List.of(
                new SuccessDetails("environment1"),
                new SuccessDetails("environment2")
        );
        List<FailureDetails> failureDetails = List.of(
                new FailureDetails("environment3", "failure message1"),
                new FailureDetails("environment4", "failure message2")
        );

        SyncOperation syncOperation = createSyncOperation(synchronizationStatus);
        syncOperation.setSuccessList(successDetails);
        syncOperation.setFailureList(failureDetails);

        SyncOperationStatus actual = underTest.convert(syncOperation);

        assertEqualsDefaults(actual);
        assertEquals(synchronizationStatus, actual.getStatus());
        assertEquals(successDetails, actual.getSuccess());
        assertEquals(failureDetails, actual.getFailure());
        assertNull(actual.getError());
    }

    @Test
    void convertFailed() {
        SynchronizationStatus synchronizationStatus = SynchronizationStatus.FAILED;

        SyncOperation syncOperation = createSyncOperation(synchronizationStatus);
        String errorMessage = "error message";
        syncOperation.setError(errorMessage);

        SyncOperationStatus actual = underTest.convert(syncOperation);

        assertEqualsDefaults(actual);
        assertEquals(synchronizationStatus, actual.getStatus());
        assertEquals(List.of(), actual.getSuccess());
        assertEquals(List.of(), actual.getFailure());
        assertEquals(errorMessage, actual.getError());
    }

    private void assertEqualsDefaults(SyncOperationStatus actual) {
        assertEquals(OPERATION_ID, actual.getOperationId());
        assertEquals(SYNC_OPERATION_TYPE, actual.getSyncOperationType());
        assertEquals(START_TIME, actual.getStartTime());
        assertEquals(END_TIME, actual.getEndTime());
    }

    private SyncOperation createSyncOperation(SynchronizationStatus status) {
        SyncOperation syncOperation = new SyncOperation();
        syncOperation.setOperationId(OPERATION_ID);
        syncOperation.setAccountId(ACCOUNT_ID);
        syncOperation.setSyncOperationType(SYNC_OPERATION_TYPE);
        syncOperation.setStatus(status);
        syncOperation.setStartTime(START_TIME);
        syncOperation.setEndTime(END_TIME);
        return syncOperation;
    }
}