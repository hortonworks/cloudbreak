package com.sequenceiq.freeipa.api.v1.operation.model;

import org.junit.Test;

import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;

public class OperationStateTest {
    @Test
    public void testMappingExistsForAllSyncOperationType() {
        for (SynchronizationStatus synchronizationStatus : SynchronizationStatus.values()) {
            OperationState.fromSynchronizationStatus(synchronizationStatus);
        }
    }
}