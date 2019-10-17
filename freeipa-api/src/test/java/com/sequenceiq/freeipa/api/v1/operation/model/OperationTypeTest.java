package com.sequenceiq.freeipa.api.v1.operation.model;

import org.junit.Test;

import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationType;

public class OperationTypeTest {

    @Test
    public void testMappingExistsForAllSyncOperationType() {
        for (SyncOperationType syncOperationType : SyncOperationType.values()) {
            OperationType.fromSyncOperationType(syncOperationType);
        }
    }
}