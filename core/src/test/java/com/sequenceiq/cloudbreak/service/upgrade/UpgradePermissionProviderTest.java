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
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.matrix.UpgradeMatrixService;

@RunWith(MockitoJUnitRunner.class)
public class UpgradePermissionProviderTest {

    private static final String VERSION_KEY = "cm";

    private static final String BUILD_NUMBER_KEY = "build-number";

    @InjectMocks
    private UpgradePermissionProvider underTest;

    @Mock
    private ComponentBuildNumberComparator componentBuildNumberComparator;

    @Mock
    private UpgradeMatrixService upgradeMatrixService;

    @Mock
    private ComponentVersionComparator componentVersionComparator;

    @Test
    public void testPermitUpgradeShouldReturnTrueWhenTheVersionsAreEqualAndTheBuildNumberIsGreater() {
        String componentVersion = "7.2.1";
        Image currentImage = createImage(componentVersion, "2000");
        Image candidateImage = createImage(componentVersion, "2001");
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, true, Map.of(), true);

        when(componentBuildNumberComparator.compare(currentImage, candidateImage, BUILD_NUMBER_KEY)).thenReturn(true);

        boolean actual = underTest.permitCmAndStackUpgrade(imageFilterParams, candidateImage, VERSION_KEY, BUILD_NUMBER_KEY);

        assertTrue(actual);
        verify(componentBuildNumberComparator).compare(currentImage, candidateImage, BUILD_NUMBER_KEY);
        verifyNoInteractions(upgradeMatrixService, componentVersionComparator);
    }

    @Test
    public void testPermitUpgradeShouldReturnFalseWhenTheVersionsAreEqualAndTheBuildNumberIsLower() {
        String componentVersion = "7.2.1";
        Image currentImage = createImage(componentVersion, "2002");
        Image candidateImage = createImage(componentVersion, "2001");
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, true, Map.of(), true);

        when(componentBuildNumberComparator.compare(currentImage, candidateImage, BUILD_NUMBER_KEY)).thenReturn(false);

        boolean actual = underTest.permitCmAndStackUpgrade(imageFilterParams, candidateImage, VERSION_KEY, BUILD_NUMBER_KEY);

        assertFalse(actual);
        verify(componentBuildNumberComparator).compare(currentImage, candidateImage, BUILD_NUMBER_KEY);
        verifyNoInteractions(upgradeMatrixService, componentVersionComparator);
    }

    @Test
    public void testPermitUpgradeShouldReturnTrueWhenTheCandidateCmVersionIsGreater() {
        String currentVersion = "7.2.1";
        String targetVersion = "7.2.2";
        Image currentImage = createImage(currentVersion, "2002");
        Image candidateImage = createImage(targetVersion, "2001");
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, true, Map.of(), true);

        when(componentVersionComparator.permitCmAndStackUpgradeByComponentVersion(currentVersion, targetVersion)).thenReturn(true);
        when(upgradeMatrixService.permitByUpgradeMatrix(currentVersion, targetVersion)).thenReturn(true);

        boolean actual = underTest.permitCmAndStackUpgrade(imageFilterParams, candidateImage, VERSION_KEY, BUILD_NUMBER_KEY);

        assertTrue(actual);
        verify(componentVersionComparator).permitCmAndStackUpgradeByComponentVersion(currentVersion, targetVersion);
        verify(upgradeMatrixService).permitByUpgradeMatrix(currentVersion, targetVersion);
        verifyNoInteractions(componentBuildNumberComparator);
    }

    @Test
    public void testPermitUpgradeShouldReturnFalseWhenTheCandidateCmVersionIsLower() {
        String currentVersion = "7.2.1";
        String targetVersion = "7.1.2";
        Image currentImage = createImage(currentVersion, "2002");
        Image candidateImage = createImage(targetVersion, "2001");
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, true, Map.of(), true);

        when(componentVersionComparator.permitCmAndStackUpgradeByComponentVersion(currentVersion, targetVersion)).thenReturn(false);

        boolean actual = underTest.permitCmAndStackUpgrade(imageFilterParams, candidateImage, VERSION_KEY, BUILD_NUMBER_KEY);

        assertFalse(actual);
        verify(componentVersionComparator).permitCmAndStackUpgradeByComponentVersion(currentVersion, targetVersion);
        verifyNoInteractions(componentBuildNumberComparator, upgradeMatrixService);
    }

    @Test
    public void testPermitUpgradeShouldReturnFalseWhenTheUpgradeNotPermittedByTheUpgradeMatrix() {
        String currentVersion = "7.2.1";
        String targetVersion = "7.2.2";
        Image currentImage = createImage(currentVersion, "2002");
        Image candidateImage = createImage(targetVersion, "2001");
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, true, Map.of(), true);

        when(componentVersionComparator.permitCmAndStackUpgradeByComponentVersion(currentVersion, targetVersion)).thenReturn(true);
        when(upgradeMatrixService.permitByUpgradeMatrix(currentVersion, targetVersion)).thenReturn(false);

        boolean actual = underTest.permitCmAndStackUpgrade(imageFilterParams, candidateImage, VERSION_KEY, BUILD_NUMBER_KEY);

        assertFalse(actual);
        verify(componentVersionComparator).permitCmAndStackUpgradeByComponentVersion(currentVersion, targetVersion);
        verify(upgradeMatrixService).permitByUpgradeMatrix(currentVersion, targetVersion);
        verifyNoInteractions(componentBuildNumberComparator);
    }

    @Test
    public void testPermitUpgradeShouldReturnFalseWhenTheCmBuildNumberIsNotAvailable() {
        Image currentImage = createImage("7.2.1", "2002");
        Image candidateImage = createImage("7.2.1", null);
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, true, Map.of(), true);

        when(componentBuildNumberComparator.compare(currentImage, candidateImage, BUILD_NUMBER_KEY)).thenReturn(false);

        boolean actual = underTest.permitCmAndStackUpgrade(imageFilterParams, candidateImage, VERSION_KEY, BUILD_NUMBER_KEY);

        assertFalse(actual);
        verify(componentBuildNumberComparator).compare(currentImage, candidateImage, BUILD_NUMBER_KEY);
        verifyNoInteractions(upgradeMatrixService, componentVersionComparator);
    }

    private Image createImage(String cmVersion, String buildNumber) {
        Map<String, String> packageVersions = new HashMap<>();
        packageVersions.put(VERSION_KEY, cmVersion);
        packageVersions.put(BUILD_NUMBER_KEY, buildNumber);
        return new Image(null, null, null, null, null, null, null, null, null, null, packageVersions, null, null, null);
    }
}
