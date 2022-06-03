package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class UpgradeDatabaseServerV4RequestTest {

    private final UpgradeDatabaseServerV4Request underTest = new UpgradeDatabaseServerV4Request();

    @Test
    void testGettersSetters() {
        underTest.setMajorVersion("majorVersion");
        assertEquals("majorVersion", underTest.getMajorVersion());
    }
}
