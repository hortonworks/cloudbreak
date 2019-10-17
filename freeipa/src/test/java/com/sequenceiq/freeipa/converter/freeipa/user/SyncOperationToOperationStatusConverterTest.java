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
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.Operation;

class SyncOperationToOperationStatusConverterTest {

    private static final String OPERATION_ID = "operationId";

    private static final String ACCOUNT_ID = "accountId";

    private static final SyncOperationType SYNC_OPERATION_TYPE = SyncOperationType.SET_PASSWORD;

    private static final OperationType OPERATION_TYPE = OperationType.SET_PASSWORD;

    private static final Long START_TIME = System.currentTimeMillis();

    private static final Long END_TIME = START_TIME + 1000L;

    private OperationToSyncOperationStatus underTest = new OperationToSyncOperationStatus();

    @Test
    void convertRunning() {
        OperationState operationState = OperationState.RUNNING;
        SynchronizationStatus synchronizationStatus = SynchronizationStatus.fromOperationState(operationState);

        Operation operation = createSyncOperation(operationState);

        SyncOperationStatus actual = underTest.convert(operation);

        assertEqualsDefaults(actual);
        assertEquals(synchronizationStatus, actual.getStatus());
        assertEquals(List.of(), actual.getSuccess());
        assertEquals(List.of(), actual.getFailure());
        assertNull(actual.getError());
    }

    @Test
    void convertCompleted() {
        OperationState operationState = OperationState.COMPLETED;
        SynchronizationStatus synchronizationStatus = SynchronizationStatus.fromOperationState(operationState);
        List<SuccessDetails> successDetails = List.of(
                new SuccessDetails("environment1"),
                new SuccessDetails("environment2")
        );
        List<FailureDetails> failureDetails = List.of(
                new FailureDetails("environment3", "failure message1"),
                new FailureDetails("environment4", "failure message2")
        );

        Operation operation = createSyncOperation(operationState);
        operation.setSuccessList(successDetails);
        operation.setFailureList(failureDetails);

        SyncOperationStatus actual = underTest.convert(operation);

        assertEqualsDefaults(actual);
        assertEquals(synchronizationStatus, actual.getStatus());
        assertEquals(successDetails, actual.getSuccess());
        assertEquals(failureDetails, actual.getFailure());
        assertNull(actual.getError());
    }

    @Test
    void convertFailed() {
        OperationState operationState = OperationState.FAILED;
        SynchronizationStatus synchronizationStatus = SynchronizationStatus.fromOperationState(operationState);

        Operation operation = createSyncOperation(operationState);
        String errorMessage = "error message";
        operation.setError(errorMessage);

        SyncOperationStatus actual = underTest.convert(operation);

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

    private Operation createSyncOperation(OperationState status) {
        Operation operation = new Operation();
        operation.setOperationId(OPERATION_ID);
        operation.setAccountId(ACCOUNT_ID);
        operation.setOperationType(OPERATION_TYPE);
        operation.setStatus(status);
        operation.setStartTime(START_TIME);
        operation.setEndTime(END_TIME);
        return operation;
    }
}