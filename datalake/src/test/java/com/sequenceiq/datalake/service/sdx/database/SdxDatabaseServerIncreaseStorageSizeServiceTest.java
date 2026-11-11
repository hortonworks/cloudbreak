package com.sequenceiq.datalake.service.sdx.database;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;

class SdxDatabaseServerIncreaseStorageSizeServiceTest {

    private final SdxDatabaseServerIncreaseStorageSizeService underTest = new SdxDatabaseServerIncreaseStorageSizeService();

    @Test
    void testStubIsNotImplementedYet() {
        assertThrows(UnsupportedOperationException.class, () -> underTest.increaseStorageSize("userCrn", NameOrCrn.ofName("name"), 100));
    }
}
