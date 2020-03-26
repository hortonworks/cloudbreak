package com.sequenceiq.cloudbreak.service.upgrade;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UpgradePermissionProviderTest {

    @InjectMocks
    private UpgradePermissionProvider underTest;

    @Test
    public void testPermitUpgradeNewerVersion() {
        assertTrue(underTest.permitCmAndSatckUpgrade("2.2.0", "2.3.0"));
        assertTrue(underTest.permitCmAndSatckUpgrade("2.2.99", "2.3.0"));
        assertTrue(underTest.permitCmAndSatckUpgrade("2.2.99", "2.99.0"));
        assertTrue(underTest.permitCmAndSatckUpgrade("2.2.99.0", "2.99.0"));
        assertTrue(underTest.permitCmAndSatckUpgrade("2.2.99.0", "2.99.0.0"));
        assertTrue(underTest.permitCmAndSatckUpgrade("2.2", "2.99.0.0"));
        assertTrue(underTest.permitCmAndSatckUpgrade("2.2.99.0-123", "2.99.0.0-123"));


        assertTrue(underTest.permitExtensionUpgrade("2.2.0", "2.3.0"));
        assertTrue(underTest.permitExtensionUpgrade("2.2.99", "2.3.0"));
        assertTrue(underTest.permitExtensionUpgrade("2.2.99", "2.99.0"));
        assertTrue(underTest.permitExtensionUpgrade("2.2.99.0", "2.99.0"));
        assertTrue(underTest.permitExtensionUpgrade("2.2.99.0", "2.99.0.0"));
        assertTrue(underTest.permitExtensionUpgrade("2.2", "2.99.0.0"));
        assertTrue(underTest.permitExtensionUpgrade("2.2.99.0-123", "2.99.0.0-123"));
    }

    @Test
    public void testPermitUpgradeSameVersion() {
        assertFalse(underTest.permitCmAndSatckUpgrade("2.2.0", "2.2.0"));

        // For extensions it is okay to have the same versions
        assertTrue(underTest.permitExtensionUpgrade("2.2.0", "2.2.0"));
    }

    @Test
    public void testPermitUpgradeUberNew() {
        // This is an uber jump in versions, we shall not allow them (at least for now)
        assertFalse(underTest.permitCmAndSatckUpgrade("2.2.0", "3.2.0"));

        assertFalse(underTest.permitExtensionUpgrade("2.2.0", "3.2.0"));
    }

    @Test
    public void testPermitCmAndSatckUpgradeUberOlder() {
        assertFalse(underTest.permitCmAndSatckUpgrade("2.3.0", "2.2.0"));
        assertFalse(underTest.permitCmAndSatckUpgrade("2.3.0", "2.2.99"));
        assertFalse(underTest.permitCmAndSatckUpgrade("2.99.0", "2.2.99"));
        assertFalse(underTest.permitCmAndSatckUpgrade("2.99.0", "2.2.99.0"));
        assertFalse(underTest.permitCmAndSatckUpgrade("2.99.0.0", "2.2.99.0"));
        assertFalse(underTest.permitCmAndSatckUpgrade("2.99.0.0", "2.2"));
        assertFalse(underTest.permitCmAndSatckUpgrade("2.99.0.0-123", "2.2.99.0-123"));

        assertFalse(underTest.permitExtensionUpgrade("2.3.0", "2.2.0"));
        assertFalse(underTest.permitExtensionUpgrade("2.3.0", "2.2.99"));
        assertFalse(underTest.permitExtensionUpgrade("2.99.0", "2.2.99"));
        assertFalse(underTest.permitExtensionUpgrade("2.99.0", "2.2.99.0"));
        assertFalse(underTest.permitExtensionUpgrade("2.99.0.0", "2.2.99.0"));
        assertFalse(underTest.permitExtensionUpgrade("2.99.0.0", "2.2"));
        assertFalse(underTest.permitExtensionUpgrade("2.99.0.0-123", "2.2.99.0-123"));
    }

    @Test
    public void testInvalid() {
        assertFalse(underTest.permitCmAndSatckUpgrade("2", "2.2.0"));
        assertFalse(underTest.permitCmAndSatckUpgrade("2.a", "2.2.0"));
        assertFalse(underTest.permitCmAndSatckUpgrade("2.2", "u"));

        assertFalse(underTest.permitCmAndSatckUpgrade(null, "2.2"));
        assertFalse(underTest.permitCmAndSatckUpgrade("2.2", null));
        assertFalse(underTest.permitCmAndSatckUpgrade(null, null));

        assertFalse(underTest.permitExtensionUpgrade("2", "2.2.0"));
        assertFalse(underTest.permitExtensionUpgrade("2.a", "2.2.0"));
        assertFalse(underTest.permitExtensionUpgrade("2.2", "u"));

        // If something disappears that is not good
        assertFalse(underTest.permitExtensionUpgrade("2.2", null));

        // With extension we don't need to be so strict
        assertTrue(underTest.permitExtensionUpgrade(null, "2.2"));
        assertTrue(underTest.permitExtensionUpgrade(null, null));
    }
}
