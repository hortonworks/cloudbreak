package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CDH_BUILD_NUMBER;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CM;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CM_BUILD_NUMBER;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.STACK;
import static com.sequenceiq.common.model.ImageCatalogPlatform.imageCatalogPlatform;
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
import com.sequenceiq.cloudbreak.service.image.ModelImageTestBuilder;
import com.sequenceiq.cloudbreak.service.runtimes.SupportedRuntimes;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.matrix.UpgradeMatrixService;

@RunWith(MockitoJUnitRunner.class)
public class UpgradePermissionProviderTest {

    private static final StackType DATALAKE_STACK_TYPE = StackType.DATALAKE;

    private static final long STACK_ID = 1L;

    private static final String CLOUD_PLATFORM = "AWS";

    private static final String REGION = "us-west-1";

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
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = createCurrentImage(componentVersion, "2000");
        Image candidateImage = createImage(componentVersion, "2001");
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, null, true, Map.of(), DATALAKE_STACK_TYPE, null, STACK_ID,
                new InternalUpgradeSettings(false, true, true), imageCatalogPlatform(CLOUD_PLATFORM),
                CLOUD_PLATFORM, REGION, false);

        when(componentBuildNumberComparator.compare(currentImage.getPackageVersions(), candidateImage.getPackageVersions(), CDH_BUILD_NUMBER.getKey()))
                .thenReturn(true);
        when(supportedRuntimes.isSupported("7.2.1")).thenReturn(true);

        boolean actual = underTest.permitStackUpgrade(imageFilterParams, candidateImage);

        assertTrue(actual);
        verify(componentBuildNumberComparator).compare(currentImage.getPackageVersions(), candidateImage.getPackageVersions(), CDH_BUILD_NUMBER.getKey());
        verifyNoInteractions(upgradeMatrixService, componentVersionComparator);
    }

    @Test
    public void testPermitStackUpgradeShouldReturnFalseWhenTheVersionsAreEqualAndTheBuildNumberIsLower() {
        String componentVersion = "7.2.1";
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = createCurrentImage(componentVersion, "2002");
        Image candidateImage = createImage(componentVersion, "2001");
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, null, true, Map.of(), DATALAKE_STACK_TYPE, null, STACK_ID,
                new InternalUpgradeSettings(false, true, true), imageCatalogPlatform(CLOUD_PLATFORM),
                CLOUD_PLATFORM, REGION, false);

        when(componentBuildNumberComparator.compare(currentImage.getPackageVersions(), candidateImage.getPackageVersions(), CDH_BUILD_NUMBER.getKey()))
                .thenReturn(false);
        when(supportedRuntimes.isSupported("7.2.1")).thenReturn(true);

        boolean actual = underTest.permitStackUpgrade(imageFilterParams, candidateImage);

        assertFalse(actual);
        verify(componentBuildNumberComparator).compare(currentImage.getPackageVersions(), candidateImage.getPackageVersions(), CDH_BUILD_NUMBER.getKey());
        verifyNoInteractions(upgradeMatrixService, componentVersionComparator);
    }

    @Test
    public void testPermitStackUpgradeShouldReturnTrueWhenTheCandidateCmVersionIsGreater() {
        String currentVersion = "7.2.1";
        String targetVersion = "7.2.2";
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = createCurrentImage(currentVersion, "2002");
        Image candidateImage = createImage(targetVersion, "2001");
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, null, true, Map.of(), DATALAKE_STACK_TYPE, null, STACK_ID,
                new InternalUpgradeSettings(false, true, true), imageCatalogPlatform(CLOUD_PLATFORM),
                CLOUD_PLATFORM, REGION, false);

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
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = createCurrentImage(currentVersion, "2002");
        Image candidateImage = createImage(targetVersion, "2001");
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, null, true, Map.of(), DATALAKE_STACK_TYPE, null, STACK_ID,
                new InternalUpgradeSettings(false, true, true), imageCatalogPlatform(CLOUD_PLATFORM),
                CLOUD_PLATFORM, REGION, false);

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
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = createCurrentImage(currentVersion, "2002");
        Image candidateImage = createImage(targetVersion, "2001");
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, null, true, Map.of(), DATALAKE_STACK_TYPE, null, STACK_ID,
                new InternalUpgradeSettings(false, true, true), imageCatalogPlatform(CLOUD_PLATFORM),
                CLOUD_PLATFORM, REGION, false);

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
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = createCurrentImage(currentVersion, "2002");
        Image candidateImage = createImage(targetVersion, "2001");
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, null, true, Map.of(), StackType.WORKLOAD, null, STACK_ID,
                new InternalUpgradeSettings(false, true, true), imageCatalogPlatform(CLOUD_PLATFORM),
                CLOUD_PLATFORM, REGION, false);

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
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = createCurrentImage(currentVersion, "2002");
        Image candidateImage = createImage(targetVersion, "2001");
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, null, true, Map.of(), DATALAKE_STACK_TYPE, null, STACK_ID,
                new InternalUpgradeSettings(false, true, true), imageCatalogPlatform(CLOUD_PLATFORM),
                CLOUD_PLATFORM, REGION, false);

        when(componentVersionComparator.permitCmAndStackUpgradeByComponentVersion(currentVersion, targetVersion)).thenReturn(true);

        boolean actual = underTest.permitCmUpgrade(imageFilterParams, candidateImage);

        assertTrue(actual);
        verify(componentVersionComparator).permitCmAndStackUpgradeByComponentVersion(currentVersion, targetVersion);
        verify(upgradeMatrixService, never()).permitByUpgradeMatrix(currentVersion, targetVersion);
        verifyNoInteractions(componentBuildNumberComparator);
    }

    @Test
    public void testPermitStackUpgradeShouldReturnFalseWhenTheCmBuildNumberIsNotAvailable() {
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = createCurrentImage("7.2.1", "2002");
        Image candidateImage = createImage("7.2.1", null);
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, null, true, Map.of(), DATALAKE_STACK_TYPE, null, STACK_ID,
                new InternalUpgradeSettings(false, true, true), imageCatalogPlatform(CLOUD_PLATFORM),
                CLOUD_PLATFORM, REGION, false);

        when(componentBuildNumberComparator.compare(currentImage.getPackageVersions(), candidateImage.getPackageVersions(), CDH_BUILD_NUMBER.getKey()))
                .thenReturn(false);
        when(supportedRuntimes.isSupported("7.2.1")).thenReturn(true);

        boolean actual = underTest.permitStackUpgrade(imageFilterParams, candidateImage);

        assertFalse(actual);
        verify(componentBuildNumberComparator).compare(currentImage.getPackageVersions(), candidateImage.getPackageVersions(), CDH_BUILD_NUMBER.getKey());
        verifyNoInteractions(upgradeMatrixService, componentVersionComparator);
    }

    @Test
    public void testPermitStackUpgradeShouldReturnfalseWhenTheCandidateCdhVersionIsNotSupported() {
        String currentVersion = "7.2.1";
        String targetVersion = "7.2.10";
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = createCurrentImage(currentVersion, "2002");
        Image candidateImage = createImage(targetVersion, "2010");
        ImageFilterParams imageFilterParams = new ImageFilterParams(currentImage, null, true, Map.of(), DATALAKE_STACK_TYPE, null, STACK_ID,
                new InternalUpgradeSettings(false, true, true), imageCatalogPlatform(CLOUD_PLATFORM),
                CLOUD_PLATFORM, REGION, false);

        when(supportedRuntimes.isSupported("7.2.10")).thenReturn(false);

        boolean actual = underTest.permitStackUpgrade(imageFilterParams, candidateImage);

        assertFalse(actual);
        verify(supportedRuntimes).isSupported("7.2.10");
        verifyNoInteractions(componentBuildNumberComparator, componentVersionComparator, upgradeMatrixService);
    }

    private Image createImage(String version, String buildNumber) {
        Map<String, String> packageVersions = createPackageVersions(version, buildNumber);
        return new Image(null, null, null, null, null, null, null, null, null, null, null, packageVersions, null, null, null, true, null, null);
    }

    private com.sequenceiq.cloudbreak.cloud.model.Image createCurrentImage(String version, String buildNumber) {
        return ModelImageTestBuilder.builder().withPackageVersions(createPackageVersions(version, buildNumber)).build();
    }

    private Map<String, String> createPackageVersions(String version, String buildNumber) {
        Map<String, String> packageVersions = new HashMap<>();
        // cm
        packageVersions.put(CM.getKey(), version);
        packageVersions.put(CM_BUILD_NUMBER.getKey(), buildNumber);
        // cdh
        packageVersions.put(STACK.getKey(), version);
        packageVersions.put(CDH_BUILD_NUMBER.getKey(), buildNumber);
        return packageVersions;
    }
}
