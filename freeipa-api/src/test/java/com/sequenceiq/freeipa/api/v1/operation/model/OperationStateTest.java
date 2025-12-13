package com.sequenceiq.freeipa.api.v1.operation.model;

import org.junit.jupiter.api.Test;

import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;

class OperationStateTest {
    @Test
    void testMappingExistsForAllSyncOperationType() {
        for (SynchronizationStatus synchronizationStatus : SynchronizationStatus.values()) {
            OperationState.fromSynchronizationStatus(synchronizationStatus);
        }
    }
}