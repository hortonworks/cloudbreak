package com.sequenceiq.cloudbreak.service.upgrade;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
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

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.service.runtimes.SupportedRuntimes;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.matrix.UpgradeMatrixService;

@RunWith(MockitoJUnitRunner.class)
public class UpgradePermissionProviderTest {

    private static final StackType DATALAKE_STACK_TYPE = StackType.DATALAKE;

    private static final long STACK_ID = 1L;

    private static final String CLOUD_PLATFORM = "AWS";

    @InjectMocks
    private UpgradePermissionProvider underTest;

    @Mock
    private ComponentBuildNumberComparator componentBuildNumberComparator;

    @Mock
    private UpgradeMatrixService upgradeMatrixService;

    @Mock
    private ComponentVersionComparator componentVersionComparator;

    @Mock
    private SupportedRuntimes supportedRuntimes;

    @Test
    public void testPermitStackUpgradeShouldReturnTrueWhenTheVersionsAreEqualAndTheBuildNumberIsGreater() {
        String componentVersion = "7.2.1";
        Image currentImage = createImage(componentVersion, "2000");
        Image candidateImage = createImage(componentVersion, "2001");
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, true, Map.of(), DATALAKE_STACK_TYPE, null, STACK_ID,
                new InternalUpgradeSettings(false, true, true), CLOUD_PLATFORM);

        when(componentBuildNumberComparator.compare(currentImage, candidateImage, ImagePackageVersion.CDH_BUILD_NUMBER.getKey())).thenReturn(true);
        when(supportedRuntimes.isSupported("7.2.1")).thenReturn(true);

        boolean actual = underTest.permitStackUpgrade(imageFilterParams, candidateImage);

        assertTrue(actual);
        verify(componentBuildNumberComparator).compare(currentImage, candidateImage, ImagePackageVersion.CDH_BUILD_NUMBER.getKey());
        verifyNoInteractions(upgradeMatrixService, componentVersionComparator);
    }

    @Test
    public void testPermitStackUpgradeShouldReturnFalseWhenTheVersionsAreEqualAndTheBuildNumberIsLower() {
        String componentVersion = "7.2.1";
        Image currentImage = createImage(componentVersion, "2002");
        Image candidateImage = createImage(componentVersion, "2001");
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, true, Map.of(), DATALAKE_STACK_TYPE, null, STACK_ID,
                new InternalUpgradeSettings(false, true, true), CLOUD_PLATFORM);

        when(componentBuildNumberComparator.compare(currentImage, candidateImage, ImagePackageVersion.CDH_BUILD_NUMBER.getKey())).thenReturn(false);
        when(supportedRuntimes.isSupported("7.2.1")).thenReturn(true);

        boolean actual = underTest.permitStackUpgrade(imageFilterParams, candidateImage);

        assertFalse(actual);
        verify(componentBuildNumberComparator).compare(currentImage, candidateImage, ImagePackageVersion.CDH_BUILD_NUMBER.getKey());
        verifyNoInteractions(upgradeMatrixService, componentVersionComparator);
    }

    @Test
    public void testPermitStackUpgradeShouldReturnTrueWhenTheCandidateCmVersionIsGreater() {
        String currentVersion = "7.2.1";
        String targetVersion = "7.2.2";
        Image currentImage = createImage(currentVersion, "2002");
        Image candidateImage = createImage(targetVersion, "2001");
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, true, Map.of(), DATALAKE_STACK_TYPE, null, STACK_ID,
                new InternalUpgradeSettings(false, true, true), CLOUD_PLATFORM);

        when(componentVersionComparator.permitCmAndStackUpgradeByComponentVersion(currentVersion, targetVersion)).thenReturn(true);
        when(upgradeMatrixService.permitByUpgradeMatrix(currentVersion, targetVersion)).thenReturn(true);
        when(supportedRuntimes.isSupported("7.2.2")).thenReturn(true);

        boolean actual = underTest.permitStackUpgrade(imageFilterParams, candidateImage);

        assertTrue(actual);
        verify(componentVersionComparator).permitCmAndStackUpgradeByComponentVersion(currentVersion, targetVersion);
        verify(upgradeMatrixService).permitByUpgradeMatrix(currentVersion, targetVersion);
        verifyNoInteractions(componentBuildNumberComparator);
    }

    @Test
    public void testPermitStackUpgradeShouldReturnFalseWhenTheCandidateCmVersionIsLower() {
        String currentVersion = "7.2.1";
        String targetVersion = "7.1.2";
        Image currentImage = createImage(currentVersion, "2002");
        Image candidateImage = createImage(targetVersion, "2001");
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, true, Map.of(), DATALAKE_STACK_TYPE, null, STACK_ID,
                new InternalUpgradeSettings(false, true, true), CLOUD_PLATFORM);

        when(componentVersionComparator.permitCmAndStackUpgradeByComponentVersion(currentVersion, targetVersion)).thenReturn(false);
        when(supportedRuntimes.isSupported("7.1.2")).thenReturn(true);

        boolean actual = underTest.permitStackUpgrade(imageFilterParams, candidateImage);

        assertFalse(actual);
        verify(componentVersionComparator).permitCmAndStackUpgradeByComponentVersion(currentVersion, targetVersion);
        verifyNoInteractions(componentBuildNumberComparator, upgradeMatrixService);
    }

    @Test
    public void testPermitStackUpgradeShouldReturnFalseWhenTheUpgradeNotPermittedByTheUpgradeMatrix() {
        String currentVersion = "7.2.1";
        String targetVersion = "7.2.2";
        Image currentImage = createImage(currentVersion, "2002");
        Image candidateImage = createImage(targetVersion, "2001");
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, true, Map.of(), DATALAKE_STACK_TYPE, null, STACK_ID,
                new InternalUpgradeSettings(false, true, true), CLOUD_PLATFORM);

        when(componentVersionComparator.permitCmAndStackUpgradeByComponentVersion(currentVersion, targetVersion)).thenReturn(true);
        when(upgradeMatrixService.permitByUpgradeMatrix(currentVersion, targetVersion)).thenReturn(false);
        when(supportedRuntimes.isSupported("7.2.2")).thenReturn(true);

        boolean actual = underTest.permitStackUpgrade(imageFilterParams, candidateImage);

        assertFalse(actual);
        verify(componentVersionComparator).permitCmAndStackUpgradeByComponentVersion(currentVersion, targetVersion);
        verify(upgradeMatrixService).permitByUpgradeMatrix(currentVersion, targetVersion);
        verifyNoInteractions(componentBuildNumberComparator);
    }

    @Test
    public void testPermitStackUpgradeShouldReturnTrueWhenTheUpgradeNotPermittedByTheUpgradeMatrixButUpgradeMatrixCheckSkippedInImageFilterParams() {
        String currentVersion = "7.2.1";
        String targetVersion = "7.2.2";
        Image currentImage = createImage(currentVersion, "2002");
        Image candidateImage = createImage(targetVersion, "2001");
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, true, Map.of(), StackType.WORKLOAD, null, STACK_ID,
                new InternalUpgradeSettings(false, true, true), CLOUD_PLATFORM);

        when(componentVersionComparator.permitCmAndStackUpgradeByComponentVersion(currentVersion, targetVersion)).thenReturn(true);
        when(supportedRuntimes.isSupported("7.2.2")).thenReturn(true);

        boolean actual = underTest.permitStackUpgrade(imageFilterParams, candidateImage);

        assertTrue(actual);
        verify(componentVersionComparator).permitCmAndStackUpgradeByComponentVersion(currentVersion, targetVersion);
        verify(upgradeMatrixService, never()).permitByUpgradeMatrix(currentVersion, targetVersion);
        verifyNoInteractions(componentBuildNumberComparator);
    }

    @Test
    public void testPermitCmUpgradeShouldReturnTrueWhenTheUpgradeNotPermittedByTheUpgradeMatrix() {
        String currentVersion = "7.2.1";
        String targetVersion = "7.2.2";
        Image currentImage = createImage(currentVersion, "2002");
        Image candidateImage = createImage(targetVersion, "2001");
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, true, Map.of(), DATALAKE_STACK_TYPE, null, STACK_ID,
                new InternalUpgradeSettings(false, true, true), CLOUD_PLATFORM);

        when(componentVersionComparator.permitCmAndStackUpgradeByComponentVersion(currentVersion, targetVersion)).thenReturn(true);

        boolean actual = underTest.permitCmUpgrade(imageFilterParams, candidateImage);

        assertTrue(actual);
        verify(componentVersionComparator).permitCmAndStackUpgradeByComponentVersion(currentVersion, targetVersion);
        verify(upgradeMatrixService, never()).permitByUpgradeMatrix(currentVersion, targetVersion);
        verifyNoInteractions(componentBuildNumberComparator);
    }

    @Test
    public void testPermitStackUpgradeShouldReturnFalseWhenTheCmBuildNumberIsNotAvailable() {
        Image currentImage = createImage("7.2.1", "2002");
        Image candidateImage = createImage("7.2.1", null);
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, true, Map.of(), DATALAKE_STACK_TYPE, null, STACK_ID,
                new InternalUpgradeSettings(false, true, true), CLOUD_PLATFORM);

        when(componentBuildNumberComparator.compare(currentImage, candidateImage, ImagePackageVersion.CDH_BUILD_NUMBER.getKey())).thenReturn(false);
        when(supportedRuntimes.isSupported("7.2.1")).thenReturn(true);

        boolean actual = underTest.permitStackUpgrade(imageFilterParams, candidateImage);

        assertFalse(actual);
        verify(componentBuildNumberComparator).compare(currentImage, candidateImage, ImagePackageVersion.CDH_BUILD_NUMBER.getKey());
        verifyNoInteractions(upgradeMatrixService, componentVersionComparator);
    }

    @Test
    public void testPermitStackUpgradeShouldReturnfalseWhenTheCandidateCdhVersionIsNotSupported() {
        String currentVersion = "7.2.1";
        String targetVersion = "7.2.10";
        Image currentImage = createImage(currentVersion, "2002");
        Image candidateImage = createImage(targetVersion, "2010");
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, true, Map.of(), DATALAKE_STACK_TYPE, null, STACK_ID,
                new InternalUpgradeSettings(false, true, true), CLOUD_PLATFORM);

        when(supportedRuntimes.isSupported("7.2.10")).thenReturn(false);

        boolean actual = underTest.permitStackUpgrade(imageFilterParams, candidateImage);

        assertFalse(actual);
        verify(supportedRuntimes).isSupported("7.2.10");
        verifyNoInteractions(componentBuildNumberComparator, componentVersionComparator, upgradeMatrixService);
    }

    private Image createImage(String version, String buildNumber) {
        Map<String, String> packageVersions = new HashMap<>();
        // cm
        packageVersions.put(ImagePackageVersion.CM.getKey(), version);
        packageVersions.put(ImagePackageVersion.CM_BUILD_NUMBER.getKey(), buildNumber);
        // cdh
        packageVersions.put(ImagePackageVersion.STACK.getKey(), version);
        packageVersions.put(ImagePackageVersion.CDH_BUILD_NUMBER.getKey(), buildNumber);
        return new Image(null, null, null, null, null, null, null, null, null, null, null, packageVersions, null, null, null, true, null, null);
    }
}
