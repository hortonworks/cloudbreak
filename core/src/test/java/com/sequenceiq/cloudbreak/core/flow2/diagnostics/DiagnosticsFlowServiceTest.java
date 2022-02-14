package com.sequenceiq.cloudbreak.core.flow2.diagnostics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DiagnosticsFlowServiceTest {

    private DiagnosticsFlowService underTest;

    @BeforeEach
    public void setUp() {
        underTest = new DiagnosticsFlowService();
    }

    @Test
    public void testIsVersionGreaterOrEqualIfVersionEquals() {
        // GIVEN
        // WHEN
        boolean result = underTest.isVersionGreaterOrEqual("0.4.8", "0.4.8");
        // THEN
        assertTrue(result);
    }

    @Test
    public void testIsVersionGreaterOrEqualIfVersionLess() {
        // GIVEN
        // WHEN
        boolean result = underTest.isVersionGreaterOrEqual("0.4.7", "0.4.8");
        // THEN
        assertFalse(result);
    }

    @Test
    public void testIsVersionGreaterOrEqualIfVersionGreater() {
        // GIVEN
        // WHEN
        boolean result = underTest.isVersionGreaterOrEqual("0.4.9", "0.4.8");
        // THEN
        assertTrue(result);
    }

}
