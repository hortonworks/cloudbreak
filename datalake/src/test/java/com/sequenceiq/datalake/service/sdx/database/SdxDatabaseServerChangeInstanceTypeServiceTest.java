package com.sequenceiq.datalake.service.sdx.database;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;

class SdxDatabaseServerChangeInstanceTypeServiceTest {

    private final SdxDatabaseServerChangeInstanceTypeService underTest = new SdxDatabaseServerChangeInstanceTypeService();

    @Test
    void testStubsAreNotImplementedYet() {
        assertThrows(UnsupportedOperationException.class, () -> underTest.changeInstanceType("userCrn", NameOrCrn.ofName("name"), "db.m5.xlarge"));
        assertThrows(UnsupportedOperationException.class, () -> underTest.getValidInstanceTypeOptions("userCrn", NameOrCrn.ofName("name")));
    }
}
