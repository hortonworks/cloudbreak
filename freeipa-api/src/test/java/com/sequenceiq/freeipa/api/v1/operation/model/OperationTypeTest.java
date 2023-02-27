package com.sequenceiq.freeipa.api.v1.operation.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationType;

public class OperationTypeTest {

    @Test
    public void testMappingExistsForAllSyncOperationType() {
        for (SyncOperationType syncOperationType : SyncOperationType.values()) {
            OperationType.fromSyncOperationType(syncOperationType);
        }
    }

    @ParameterizedTest
    @EnumSource(value = OperationType.class)
    void testLowerCaseName(OperationType operationType) {
        assertEquals(operationType.name().toLowerCase(), operationType.getLowerCaseName());
    }

}