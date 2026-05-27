package com.sequenceiq.cloudbreak.service.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        DbOverrideVersion deprecated = new DbOverrideVersion();
        deprecated.setMinRuntimeVersion("7.3.2");
        deprecated.setEngineVersion("11");
        underTest.setDeprecatedVersions(List.of(deprecated));

        Map<String, LocalDate> eolDates = new HashMap<>();
        eolDates.put("11", LocalDate.of(2023, 11, 9));
        underTest.setEolDates(eolDates);
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

    @Test
    void testIsVersionSupportedForRuntimeWhenUnsupported() {
        assertFalse(underTest.isVersionSupportedForRuntime("11", "7.3.2"));
    }

    @Test
    void testIsVersionSupportedForRuntimeWhenSupported() {
        assertTrue(underTest.isVersionSupportedForRuntime("14", "7.3.2"));
        assertTrue(underTest.isVersionSupportedForRuntime("11", "7.2.7"));
    }

    @Test
    void testIsVersionSupportedForRuntimeWhenNoDeprecatedVersions() {
        underTest.setDeprecatedVersions(List.of());
        assertTrue(underTest.isVersionSupportedForRuntime("11", "7.3.2"));
    }

    @Test
    void testIsVersionSupportedForRuntimeWhenRuntimeIsNull() {
        assertTrue(underTest.isVersionSupportedForRuntime("11", null));
    }

    @Test
    void testIsVersionSupportedForRuntimeThresholdNewerRuntime() {
        assertFalse(underTest.isVersionSupportedForRuntime("11", "7.3.3"));
        assertFalse(underTest.isVersionSupportedForRuntime("11", "7.4.0"));
    }

    @Test
    void testIsVersionSupportedForRuntimeThresholdOlderRuntime() {
        assertTrue(underTest.isVersionSupportedForRuntime("11", "7.3.1"));
        assertTrue(underTest.isVersionSupportedForRuntime("11", "7.2.7"));
    }

    @Test
    void testGetEolDateReturnsDateWhenPresent() {
        Optional<LocalDate> result = underTest.getEolDate("11");
        assertTrue(result.isPresent());
        assertEquals(LocalDate.of(2023, 11, 9), result.get());
    }

    @Test
    void testGetEolDateReturnsEmptyWhenNotConfigured() {
        Optional<LocalDate> result = underTest.getEolDate("14");
        assertFalse(result.isPresent());
    }

    @Test
    void testIsVersionEolWhenPastEol() {
        assertTrue(underTest.isVersionEol("11", LocalDate.of(2024, 1, 1)));
    }

    @Test
    void testIsVersionEolWhenOnEolDate() {
        assertTrue(underTest.isVersionEol("11", LocalDate.of(2023, 11, 9)));
    }

    @Test
    void testIsVersionEolWhenBeforeEol() {
        assertFalse(underTest.isVersionEol("11", LocalDate.of(2023, 10, 1)));
    }

    @Test
    void testIsVersionEolWhenNoEolConfigured() {
        assertFalse(underTest.isVersionEol("14", LocalDate.of(2030, 1, 1)));
    }
}
