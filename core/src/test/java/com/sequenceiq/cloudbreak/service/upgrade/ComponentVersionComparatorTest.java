package com.sequenceiq.cloudbreak.service.upgrade;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ComponentVersionComparatorTest {

    private final ComponentVersionComparator underTest = new ComponentVersionComparator();

    @Test
    public void testPermitUpgradeNewerVersion() {
        assertTrue(underTest.permitCmAndStackUpgradeByComponentVersion("2.2.0", "2.3.0"));
        assertTrue(underTest.permitCmAndStackUpgradeByComponentVersion("2.2.99", "2.3.0"));
        assertTrue(underTest.permitCmAndStackUpgradeByComponentVersion("2.2.99", "2.99.0"));
        assertTrue(underTest.permitCmAndStackUpgradeByComponentVersion("2.2.99.0", "2.99.0"));
        assertTrue(underTest.permitCmAndStackUpgradeByComponentVersion("2.2.99.0", "2.99.0.0"));
        assertTrue(underTest.permitCmAndStackUpgradeByComponentVersion("2.2", "2.99.0.0"));
        assertTrue(underTest.permitCmAndStackUpgradeByComponentVersion("2.2.99.0-123", "2.99.0.0-123"));
    }

    @Test
    public void testPermitUpgradeSameVersion() {
        assertFalse(underTest.permitCmAndStackUpgradeByComponentVersion("2.2.0", "2.2.0"));
    }

    @Test
    public void testPermitUpgradeUberNew() {
        // This is an uber jump in versions, we shall not allow them (at least for now)
        assertFalse(underTest.permitCmAndStackUpgradeByComponentVersion("2.2.0", "3.2.0"));
    }

    @Test
    public void testPermitCmAndSatckUpgradeUberOlder() {
        assertFalse(underTest.permitCmAndStackUpgradeByComponentVersion("2.3.0", "2.2.0"));
        assertFalse(underTest.permitCmAndStackUpgradeByComponentVersion("2.3.0", "2.2.99"));
        assertFalse(underTest.permitCmAndStackUpgradeByComponentVersion("2.99.0", "2.2.99"));
        assertFalse(underTest.permitCmAndStackUpgradeByComponentVersion("2.99.0", "2.2.99.0"));
        assertFalse(underTest.permitCmAndStackUpgradeByComponentVersion("2.99.0.0", "2.2.99.0"));
        assertFalse(underTest.permitCmAndStackUpgradeByComponentVersion("2.99.0.0", "2.2"));
        assertFalse(underTest.permitCmAndStackUpgradeByComponentVersion("2.99.0.0-123", "2.2.99.0-123"));
    }

    @Test
    public void testInvalid() {
        assertFalse(underTest.permitCmAndStackUpgradeByComponentVersion("2.a", "2.2.0"));
        assertFalse(underTest.permitCmAndStackUpgradeByComponentVersion("2.2", "u"));

        assertFalse(underTest.permitCmAndStackUpgradeByComponentVersion(null, "2.2"));
        assertFalse(underTest.permitCmAndStackUpgradeByComponentVersion("2.2", null));
        assertFalse(underTest.permitCmAndStackUpgradeByComponentVersion(null, null));
    }

}