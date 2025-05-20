package com.sequenceiq.cloudbreak.service.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DbOverrideConfigTest {
    private DbOverrideConfig underTest;

    @BeforeEach
    void setUp() {
        underTest = new DbOverrideConfig();
        DbOverrideVersion dbOverrideVersion1 = new DbOverrideVersion();
        dbOverrideVersion1.setMinRuntimeVersion("1.0.0");
        dbOverrideVersion1.setEngineVersion("2");
        DbOverrideVersion dbOverrideVersion2 = new DbOverrideVersion();
        dbOverrideVersion2.setMinRuntimeVersion("2.0.0");
        dbOverrideVersion2.setEngineVersion("3");
        underTest.setVersions(List.of(dbOverrideVersion1, dbOverrideVersion2));
    }

    @Test
    void testFindEngineVersionForRuntime() {
        String result = underTest.findEngineVersionForRuntime("0.9.0");
        assertEquals(null, result);

        result = underTest.findEngineVersionForRuntime("1.0.0");
        assertEquals("2", result);

        result = underTest.findEngineVersionForRuntime("1.5.0");
        assertEquals("2", result);

        result = underTest.findEngineVersionForRuntime("2.0.0");
        assertEquals("3", result);

        result = underTest.findEngineVersionForRuntime("2.5.0");
        assertEquals("3", result);

        result = underTest.findEngineVersionForRuntime("3.0.0");
        assertEquals("3", result);
    }

    @Test
    void testFindMinEngineVersion() {
        String result = underTest.findMinEngineVersion();
        assertEquals("2", result);
    }

    @Test
    void testFindMinRuntimeVersion() {
        Optional<String> result = underTest.findMinRuntimeVersion("1");
        assertFalse(result.isPresent());

        result = underTest.findMinRuntimeVersion("2");
        assertTrue(result.isPresent());
        assertEquals("1.0.0", result.get());

        result = underTest.findMinRuntimeVersion("3");
        assertTrue(result.isPresent());
        assertEquals("2.0.0", result.get());

        result = underTest.findMinRuntimeVersion("4");
        assertFalse(result.isPresent());
    }
}
