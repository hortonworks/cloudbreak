package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CDH_BUILD_NUMBER;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CM;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CM_BUILD_NUMBER;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.STACK;
import static com.sequenceiq.common.model.ImageCatalogPlatform.imageCatalogPlatform;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.runtimes.SupportedRuntimes;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.matrix.UpgradeMatrixService;

@ExtendWith(MockitoExtension.class)
public class UpgradePermissionProviderTest {

    private static final StackType DATALAKE_STACK_TYPE = StackType.DATALAKE;

    private static final long STACK_ID = 1L;

    private static final String CLOUD_PLATFORM = "AWS";

    private static final String REGION = "us-west-1";

    @InjectMocks
    private UpgradePermissionProvider underTest;

    @Mock
    private UpgradeMatrixService upgradeMatrixService;

    @Spy
    private ComponentVersionComparator componentVersionComparator;

    @Mock
    private SupportedRuntimes supportedRuntimes;

    @Mock
    private VersionComparisonContextFactory versionComparisonContextFactory;

    @Mock
    private UpgradePathRestrictionService upgradePathRestrictionService;

    @Test
    public void testPermitStackUpgradeShouldReturnTrueWhenTheVersionsAreEqualAndTheBuildNumberIsGreater() {
        String componentVersion = "7.2.1";
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = createCurrentImage(componentVersion, "2000");
        Image candidateImage = createCandidateImage(componentVersion, "2001");
        ImageFilterParams imageFilterParams = createImageFilterParams(currentImage, DATALAKE_STACK_TYPE);

        when(versionComparisonContextFactory.buildForStack(imageFilterParams.getCurrentImage().getPackageVersions(), imageFilterParams.getStackRelatedParcels()))
                .thenReturn(createVersionComparisonContext(componentVersion, "2000"));
        when(versionComparisonContextFactory.buildForStack(candidateImage))
                .thenReturn(createVersionComparisonContext(componentVersion, "2001"));
        when(supportedRuntimes.isSupported("7.2.1")).thenReturn(true);
        when(upgradePathRestrictionService.permitUpgrade(any(), any())).thenReturn(true);

        assertTrue(underTest.permitStackUpgrade(imageFilterParams, candidateImage));
        verifyNoInteractions(upgradeMatrixService);
    }

    private VersionComparisonContext createVersionComparisonContext(String componentVersion, String buildNumber) {
        return new VersionComparisonContext.Builder()
                .withMajorVersion(componentVersion)
                .withPatchVersion(null)
                .withBuildNumber(Optional.ofNullable(buildNumber).map(Integer::parseInt).orElse(null))
                .build();
    }

    private ImageFilterParams createImageFilterParams(com.sequenceiq.cloudbreak.cloud.model.Image currentImage, StackType datalakeStackType) {
        return new ImageFilterParams(null, currentImage, null, true, false, Map.of(), datalakeStackType, null, STACK_ID,
                new InternalUpgradeSettings(false, true, true), imageCatalogPlatform(CLOUD_PLATFORM),
                CLOUD_PLATFORM, REGION, false);
    }

    @Test
    public void testPermitStackUpgradeShouldReturnFalseWhenTheVersionsAreEqualAndTheBuildNumberIsLower() {
        String componentVersion = "7.2.1";
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = createCurrentImage(componentVersion, "2002");
        Image candidateImage = createCandidateImage(componentVersion, "2001");
        ImageFilterParams imageFilterParams = createImageFilterParams(currentImage, DATALAKE_STACK_TYPE);

        when(versionComparisonContextFactory.buildForStack(imageFilterParams.getCurrentImage().getPackageVersions(), imageFilterParams.getStackRelatedParcels()))
                .thenReturn(createVersionComparisonContext(componentVersion, "2002"));
        when(versionComparisonContextFactory.buildForStack(candidateImage))
                .thenReturn(createVersionComparisonContext(componentVersion, "2001"));
        when(supportedRuntimes.isSupported("7.2.1")).thenReturn(true);

        assertFalse(underTest.permitStackUpgrade(imageFilterParams, candidateImage));
        verifyNoInteractions(upgradeMatrixService);
    }

    @Test
    public void testPermitStackUpgradeShouldReturnTrueWhenTheCandidateCmVersionIsGreater() {
        String currentVersion = "7.2.1";
        String targetVersion = "7.2.2";
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = createCurrentImage(currentVersion, "2002");
        Image candidateImage = createCandidateImage(targetVersion, "2001");
        ImageFilterParams imageFilterParams = createImageFilterParams(currentImage, DATALAKE_STACK_TYPE);

        when(versionComparisonContextFactory.buildForStack(imageFilterParams.getCurrentImage().getPackageVersions(), imageFilterParams.getStackRelatedParcels()))
                .thenReturn(createVersionComparisonContext(currentVersion, "2002"));
        when(versionComparisonContextFactory.buildForStack(candidateImage))
                .thenReturn(createVersionComparisonContext(targetVersion, "2001"));
        when(upgradeMatrixService.permitByUpgradeMatrix(currentVersion, targetVersion)).thenReturn(true);
        when(supportedRuntimes.isSupported("7.2.2")).thenReturn(true);
        when(upgradePathRestrictionService.permitUpgrade(any(), any())).thenReturn(true);

        assertTrue(underTest.permitStackUpgrade(imageFilterParams, candidateImage));
        verify(componentVersionComparator).permitCmAndStackUpgradeByComponentVersion(any(), any());
        verify(upgradeMatrixService).permitByUpgradeMatrix(currentVersion, targetVersion);
    }

    @Test
    public void testPermitStackUpgradeShouldReturnFalseWhenTheUpgradePathIsNotSupported() {
        String currentVersion = "7.2.1";
        String targetVersion = "7.2.2";
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = createCurrentImage(currentVersion, "2002");
        Image candidateImage = createCandidateImage(targetVersion, "2001");
        ImageFilterParams imageFilterParams = createImageFilterParams(currentImage, DATALAKE_STACK_TYPE);

        when(versionComparisonContextFactory.buildForStack(imageFilterParams.getCurrentImage().getPackageVersions(), imageFilterParams.getStackRelatedParcels()))
                .thenReturn(createVersionComparisonContext(currentVersion, "2002"));
        when(versionComparisonContextFactory.buildForStack(candidateImage))
                .thenReturn(createVersionComparisonContext(targetVersion, "2001"));
        when(supportedRuntimes.isSupported("7.2.2")).thenReturn(true);
        when(upgradeMatrixService.permitByUpgradeMatrix(currentVersion, targetVersion)).thenReturn(true);
        when(upgradePathRestrictionService.permitUpgrade(any(), any())).thenReturn(false);

        assertFalse(underTest.permitStackUpgrade(imageFilterParams, candidateImage));
        verify(componentVersionComparator).permitCmAndStackUpgradeByComponentVersion(any(), any());
    }

    @Test
    public void testPermitStackUpgradeShouldReturnFalseWhenTheCandidateCmVersionIsLower() {
        String currentVersion = "7.2.1";
        String targetVersion = "7.1.2";
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = createCurrentImage(currentVersion, "2002");
        Image candidateImage = createCandidateImage(targetVersion, "2001");
        ImageFilterParams imageFilterParams = createImageFilterParams(currentImage, DATALAKE_STACK_TYPE);

        when(versionComparisonContextFactory.buildForStack(imageFilterParams.getCurrentImage().getPackageVersions(), imageFilterParams.getStackRelatedParcels()))
                .thenReturn(createVersionComparisonContext(currentVersion, "2002"));
        when(versionComparisonContextFactory.buildForStack(candidateImage))
                .thenReturn(createVersionComparisonContext(targetVersion, "2001"));
        when(supportedRuntimes.isSupported("7.1.2")).thenReturn(true);

        assertFalse(underTest.permitStackUpgrade(imageFilterParams, candidateImage));
        verify(componentVersionComparator).permitCmAndStackUpgradeByComponentVersion(any(), any());
    }

    @Test
    public void testPermitStackUpgradeShouldReturnFalseWhenTheUpgradeNotPermittedByTheUpgradeMatrix() {
        String currentVersion = "7.2.1";
        String targetVersion = "7.2.2";
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = createCurrentImage(currentVersion, "2002");
        Image candidateImage = createCandidateImage(targetVersion, "2001");
        ImageFilterParams imageFilterParams = createImageFilterParams(currentImage, DATALAKE_STACK_TYPE);

        when(versionComparisonContextFactory.buildForStack(imageFilterParams.getCurrentImage().getPackageVersions(), imageFilterParams.getStackRelatedParcels()))
                .thenReturn(createVersionComparisonContext(currentVersion, "2002"));
        when(versionComparisonContextFactory.buildForStack(candidateImage))
                .thenReturn(createVersionComparisonContext(targetVersion, "2001"));
        when(upgradeMatrixService.permitByUpgradeMatrix(currentVersion, targetVersion)).thenReturn(false);
        when(supportedRuntimes.isSupported("7.2.2")).thenReturn(true);

        assertFalse(underTest.permitStackUpgrade(imageFilterParams, candidateImage));
        verify(componentVersionComparator).permitCmAndStackUpgradeByComponentVersion(any(), any());
        verify(upgradeMatrixService).permitByUpgradeMatrix(currentVersion, targetVersion);
    }

    @Test
    public void testPermitStackUpgradeShouldReturnTrueWhenTheUpgradeNotPermittedByTheUpgradeMatrixButUpgradeMatrixCheckSkippedInImageFilterParams() {
        String currentVersion = "7.2.1";
        String targetVersion = "7.2.2";
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = createCurrentImage(currentVersion, "2002");
        Image candidateImage = createCandidateImage(targetVersion, "2001");
        ImageFilterParams imageFilterParams = createImageFilterParams(currentImage, StackType.WORKLOAD);

        when(versionComparisonContextFactory.buildForStack(imageFilterParams.getCurrentImage().getPackageVersions(), imageFilterParams.getStackRelatedParcels()))
                .thenReturn(createVersionComparisonContext(currentVersion, "2002"));
        when(versionComparisonContextFactory.buildForStack(candidateImage))
                .thenReturn(createVersionComparisonContext(targetVersion, "2001"));
        when(supportedRuntimes.isSupported("7.2.2")).thenReturn(true);
        when(upgradePathRestrictionService.permitUpgrade(any(), any())).thenReturn(true);

        assertTrue(underTest.permitStackUpgrade(imageFilterParams, candidateImage));
        verify(componentVersionComparator).permitCmAndStackUpgradeByComponentVersion(any(), any());
        verify(upgradeMatrixService, never()).permitByUpgradeMatrix(currentVersion, targetVersion);
    }

    @Test
    public void testPermitCmUpgradeShouldReturnTrueWhenTheUpgradeNotPermittedByTheUpgradeMatrix() {
        String currentVersion = "7.2.1";
        String targetVersion = "7.2.2";
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = createCurrentImage(currentVersion, "2002");
        Image candidateImage = createCandidateImage(targetVersion, "2001");
        ImageFilterParams imageFilterParams = createImageFilterParams(currentImage, DATALAKE_STACK_TYPE);

        when(versionComparisonContextFactory.buildForCm(imageFilterParams.getCurrentImage().getPackageVersions()))
                .thenReturn(createVersionComparisonContext(currentVersion, "2002"));
        when(versionComparisonContextFactory.buildForCm(candidateImage.getPackageVersions()))
                .thenReturn(createVersionComparisonContext(targetVersion, "2001"));

        assertTrue(underTest.permitCmUpgrade(imageFilterParams, candidateImage));
        verify(componentVersionComparator).permitCmAndStackUpgradeByComponentVersion(any(), any());
        verify(upgradeMatrixService, never()).permitByUpgradeMatrix(currentVersion, targetVersion);
    }

    @Test
    public void testPermitStackUpgradeShouldReturnFalseWhenTheCmBuildNumberIsNotAvailable() {
        String version = "7.2.1";
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = createCurrentImage(version, "2002");
        Image candidateImage = createCandidateImage(version, null);
        ImageFilterParams imageFilterParams = createImageFilterParams(currentImage, DATALAKE_STACK_TYPE);

        when(versionComparisonContextFactory.buildForStack(imageFilterParams.getCurrentImage().getPackageVersions(), imageFilterParams.getStackRelatedParcels()))
                .thenReturn(createVersionComparisonContext(version, "2002"));
        when(versionComparisonContextFactory.buildForStack(candidateImage))
                .thenReturn(createVersionComparisonContext(version, null));

        when(supportedRuntimes.isSupported(version)).thenReturn(true);

        assertFalse(underTest.permitStackUpgrade(imageFilterParams, candidateImage));
        verifyNoInteractions(upgradeMatrixService);
    }

    @Test
    public void testPermitStackUpgradeShouldReturnFalseWhenTheCandidateCdhVersionIsNotSupported() {
        String currentVersion = "7.2.1";
        String targetVersion = "7.2.10";
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = createCurrentImage(currentVersion, "2002");
        Image candidateImage = createCandidateImage(targetVersion, "2010");
        ImageFilterParams imageFilterParams = createImageFilterParams(currentImage, DATALAKE_STACK_TYPE);

        when(versionComparisonContextFactory.buildForStack(imageFilterParams.getCurrentImage().getPackageVersions(), imageFilterParams.getStackRelatedParcels()))
                .thenReturn(createVersionComparisonContext(currentVersion, "2002"));
        when(versionComparisonContextFactory.buildForStack(candidateImage))
                .thenReturn(createVersionComparisonContext(targetVersion, "2010"));
        when(supportedRuntimes.isSupported(targetVersion)).thenReturn(false);

        assertFalse(underTest.permitStackUpgrade(imageFilterParams, candidateImage));
        verify(supportedRuntimes).isSupported(targetVersion);
        verifyNoInteractions(upgradeMatrixService);
    }

    private Image createCandidateImage(String version, String buildNumber) {
        Map<String, String> packageVersions = createPackageVersions(version, buildNumber);
        return Image.builder().withPackageVersions(packageVersions).build();
    }

    private com.sequenceiq.cloudbreak.cloud.model.Image createCurrentImage(String version, String buildNumber) {
        return com.sequenceiq.cloudbreak.cloud.model.Image.builder().withPackageVersions(createPackageVersions(version, buildNumber)).build();
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
