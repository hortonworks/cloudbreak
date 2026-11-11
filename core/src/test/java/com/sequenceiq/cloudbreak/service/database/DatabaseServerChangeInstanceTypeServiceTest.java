package com.sequenceiq.cloudbreak.service.database;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;

class DatabaseServerChangeInstanceTypeServiceTest {

    private final DatabaseServerChangeInstanceTypeService underTest = new DatabaseServerChangeInstanceTypeService();

    @Test
    void testStubsAreNotImplementedYet() {
        assertThrows(UnsupportedOperationException.class, () -> underTest.changeInstanceType(NameOrCrn.ofName("name"), "accountId", "db.m5.xlarge"));
        assertThrows(UnsupportedOperationException.class, () -> underTest.getValidInstanceTypeOptions(NameOrCrn.ofName("name"), "accountId"));
    }
}
