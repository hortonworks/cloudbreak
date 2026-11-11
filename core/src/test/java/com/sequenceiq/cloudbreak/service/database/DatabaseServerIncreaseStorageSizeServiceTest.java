package com.sequenceiq.cloudbreak.service.database;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;

class DatabaseServerIncreaseStorageSizeServiceTest {

    private final DatabaseServerIncreaseStorageSizeService underTest = new DatabaseServerIncreaseStorageSizeService();

    @Test
    void testStubIsNotImplementedYet() {
        assertThrows(UnsupportedOperationException.class, () -> underTest.increaseStorageSize(NameOrCrn.ofName("name"), "accountId", 100));
    }
}
