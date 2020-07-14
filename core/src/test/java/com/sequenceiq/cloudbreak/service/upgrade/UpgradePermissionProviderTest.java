package com.sequenceiq.cloudbreak.service.upgrade;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

@RunWith(MockitoJUnitRunner.class)
public class UpgradePermissionProviderTest {

    private static final String VERSION_KEY = "cm";

    private static final String BUILD_NUMBER_KEY = "build-number";

    @InjectMocks
    private UpgradePermissionProvider underTest;

    @Mock
    private ComponentBuildNumberComparator componentBuildNumberComparator;

    @Test
    public void testPermitUpgradeNewerVersion() {
        assertTrue(underTest.permitCmAndStackUpgradeByComponentVersion("2.2.0", "2.3.0"));
        assertTrue(underTest.permitCmAndStackUpgradeByComponentVersion("2.2.99", "2.3.0"));
        assertTrue(underTest.permitCmAndStackUpgradeByComponentVersion("2.2.99", "2.99.0"));
        assertTrue(underTest.permitCmAndStackUpgradeByComponentVersion("2.2.99.0", "2.99.0"));
        assertTrue(underTest.permitCmAndStackUpgradeByComponentVersion("2.2.99.0", "2.99.0.0"));
        assertTrue(underTest.permitCmAndStackUpgradeByComponentVersion("2.2", "2.99.0.0"));
        assertTrue(underTest.permitCmAndStackUpgradeByComponentVersion("2.2.99.0-123", "2.99.0.0-123"));

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
        assertFalse(underTest.permitCmAndStackUpgradeByComponentVersion("2.2.0", "2.2.0"));

        // For extensions it is okay to have the same versions
        assertTrue(underTest.permitExtensionUpgrade("2.2.0", "2.2.0"));
    }

    @Test
    public void testPermitUpgradeUberNew() {
        // This is an uber jump in versions, we shall not allow them (at least for now)
        assertFalse(underTest.permitCmAndStackUpgradeByComponentVersion("2.2.0", "3.2.0"));

        assertFalse(underTest.permitExtensionUpgrade("2.2.0", "3.2.0"));
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
        assertFalse(underTest.permitCmAndStackUpgradeByComponentVersion("2", "2.2.0"));
        assertFalse(underTest.permitCmAndStackUpgradeByComponentVersion("2.a", "2.2.0"));
        assertFalse(underTest.permitCmAndStackUpgradeByComponentVersion("2.2", "u"));

        assertFalse(underTest.permitCmAndStackUpgradeByComponentVersion(null, "2.2"));
        assertFalse(underTest.permitCmAndStackUpgradeByComponentVersion("2.2", null));
        assertFalse(underTest.permitCmAndStackUpgradeByComponentVersion(null, null));

        assertFalse(underTest.permitExtensionUpgrade("2", "2.2.0"));
        assertFalse(underTest.permitExtensionUpgrade("2.a", "2.2.0"));
        assertFalse(underTest.permitExtensionUpgrade("2.2", "u"));

        // If something disappears that is not good
        assertFalse(underTest.permitExtensionUpgrade("2.2", null));

        // With extension we don't need to be so strict
        assertTrue(underTest.permitExtensionUpgrade(null, "2.2"));
        assertTrue(underTest.permitExtensionUpgrade(null, null));
    }

    @Test
    public void testSaltUpgrade() {
        // Allow upgrade between 2017.7.*
        assertTrue(underTest.permitSaltUpgrade("2017.7.5", "2017.7.5"));
        assertTrue(underTest.permitSaltUpgrade("2017.7.5", "2017.7.7"));
        assertTrue(underTest.permitSaltUpgrade("2017.7.7", "2017.7.5"));

        assertTrue(underTest.permitSaltUpgrade("2017.7.7", "2017.8.0"));
        assertFalse(underTest.permitSaltUpgrade("2017.7.7", "2019.7.0"));

        // Allow upgrade between 3000.*, according to new Salt versioning scheme (since
        // then, version numbers are in the format MAJOR.PATCH)
        assertTrue(underTest.permitSaltUpgrade("3000.2", "3000.2"));
        assertTrue(underTest.permitSaltUpgrade("3000.2", "3000.3"));
        assertFalse(underTest.permitSaltUpgrade("3000.2", "3001.3"));
        assertFalse(underTest.permitSaltUpgrade("3000", "3001"));

        // Do not allow if no Salt version
        assertFalse(underTest.permitSaltUpgrade("2017.7.7", null));
    }

    @Test
    public void testPermitUpgradeShouldReturnTrueWhenTheVersionsAreEqualAndTheBuildNumberIsGreater() {
        Image currentImage = createImage("7.2.1", "2000");
        Image candidateImage = createImage("7.2.1", "2001");

        when(componentBuildNumberComparator.compare(currentImage, candidateImage, BUILD_NUMBER_KEY)).thenReturn(true);

        boolean actual = underTest.permitCmAndStackUpgrade(currentImage, candidateImage, VERSION_KEY, BUILD_NUMBER_KEY);

        assertTrue(actual);
        verify(componentBuildNumberComparator).compare(currentImage, candidateImage, BUILD_NUMBER_KEY);
    }

    @Test
    public void testPermitUpgradeShouldReturnFalseWhenTheVersionsAreEqualAndTheBuildNumberIsLower() {
        Image currentImage = createImage("7.2.1", "2002");
        Image candidateImage = createImage("7.2.1", "2001");

        when(componentBuildNumberComparator.compare(currentImage, candidateImage, BUILD_NUMBER_KEY)).thenReturn(false);

        boolean actual = underTest.permitCmAndStackUpgrade(currentImage, candidateImage, VERSION_KEY, BUILD_NUMBER_KEY);

        assertFalse(actual);
        verify(componentBuildNumberComparator).compare(currentImage, candidateImage, BUILD_NUMBER_KEY);
    }

    @Test
    public void testPermitUpgradeShouldReturnTrueWhenTheCandidateCmVersionIsGreater() {
        Image currentImage = createImage("7.2.1", "2002");
        Image candidateImage = createImage("7.2.2", "2001");

        boolean actual = underTest.permitCmAndStackUpgrade(currentImage, candidateImage, VERSION_KEY, BUILD_NUMBER_KEY);

        assertTrue(actual);
        verifyNoInteractions(componentBuildNumberComparator);
    }

    @Test
    public void testPermitUpgradeShouldReturnFalseWhenTheCmBuildNumberIsNotAvailable() {
        Image currentImage = createImage("7.2.1", "2002");
        Image candidateImage = createImage("7.2.1", null);

        when(componentBuildNumberComparator.compare(currentImage, candidateImage, BUILD_NUMBER_KEY)).thenReturn(false);

        boolean actual = underTest.permitCmAndStackUpgrade(currentImage, candidateImage, VERSION_KEY, BUILD_NUMBER_KEY);

        assertFalse(actual);
        verify(componentBuildNumberComparator).compare(currentImage, candidateImage, BUILD_NUMBER_KEY);
    }

    private Image createImage(String cmVersion, String buildNumber) {
        Map<String, String> packageVersions = new HashMap<>();
        packageVersions.put(VERSION_KEY, cmVersion);
        packageVersions.put(BUILD_NUMBER_KEY, buildNumber);
        return new Image(null, null, null, null, null, null, null, null, null, null, packageVersions, null, null, null);
    }
}
