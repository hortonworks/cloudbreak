package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import static com.sequenceiq.common.model.ImageCatalogPlatform.imageCatalogPlatform;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradePermissionProvider;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;
import com.sequenceiq.cloudbreak.service.upgrade.image.locked.LockedComponentChecker;

@ExtendWith(MockitoExtension.class)
class CmAndStackVersionUpgradeImageFilterTest {

    private static final String V_7_0_3 = "7.0.3";

    private static final String BUILD_NUMBER = "12345";

    private final Map<String, String> activatedParcels = Map.of("stack", V_7_0_3);

    @Mock
    private LockedComponentChecker lockedComponentChecker;

    @Mock
    private UpgradePermissionProvider upgradePermissionProvider;

    @InjectMocks
    private CmAndStackVersionUpgradeImageFilter underTest;

    @Mock
    private Image candidateImage;

    @BeforeEach
    public void setUp() {
        lenient().when(candidateImage.getOs()).thenReturn("centos7");
        lenient().when(candidateImage.getOsType()).thenReturn("redhat7");
    }

    @Test
    public void testFilterShouldReturnErrorMessageWhenNotLockedAndStackPermitCheckIsFalse() {
        ImageFilterParams imageFilterParams = createImageFilterParams(false);
        when(upgradePermissionProvider.permitStackUpgrade(imageFilterParams, candidateImage)).thenReturn(Boolean.FALSE);
        when(upgradePermissionProvider.permitCmUpgrade(imageFilterParams, candidateImage)).thenReturn(Boolean.TRUE);

        ImageFilterResult actual = underTest.filter(new ImageFilterResult(List.of(candidateImage)), imageFilterParams);

        assertNotLockedCommon(actual);
    }

    @Test
    public void testFilterShouldReturnErrorMessageWhenNotLockedAndCMPermitCheckIsFalse() {
        ImageFilterParams imageFilterParams = createImageFilterParams(false);
        lenient().when(upgradePermissionProvider.permitStackUpgrade(imageFilterParams, candidateImage)).thenReturn(Boolean.TRUE);
        when(upgradePermissionProvider.permitCmUpgrade(imageFilterParams, candidateImage)).thenReturn(Boolean.FALSE);

        ImageFilterResult actual = underTest.filter(new ImageFilterResult(List.of(candidateImage)), imageFilterParams);

        assertNotLockedCommon(actual);
    }

    @Test
    public void testFilterShouldReturnFalseWhenNotLockedAndCMAndStackPermitCheckIsFalse() {
        ImageFilterParams imageFilterParams = createImageFilterParams(false);
        lenient().when(upgradePermissionProvider.permitStackUpgrade(imageFilterParams, candidateImage)).thenReturn(Boolean.FALSE);
        when(upgradePermissionProvider.permitCmUpgrade(imageFilterParams, candidateImage)).thenReturn(Boolean.FALSE);

        ImageFilterResult actual = underTest.filter(new ImageFilterResult(List.of(candidateImage)), imageFilterParams);

        assertNotLockedCommon(actual);
    }

    @Test
    public void testFilterShouldReturnTrueWhenNotLockedAndCMAndStackPermitCheckIsTrue() {
        ImageFilterParams imageFilterParams = createImageFilterParams(false);
        when(upgradePermissionProvider.permitStackUpgrade(imageFilterParams, candidateImage)).thenReturn(Boolean.TRUE);
        when(upgradePermissionProvider.permitCmUpgrade(imageFilterParams, candidateImage)).thenReturn(Boolean.TRUE);

        ImageFilterResult actual = underTest.filter(new ImageFilterResult(List.of(candidateImage)), imageFilterParams);

        assertFalse(actual.getImages().isEmpty());
        assertTrue(actual.getReason().isEmpty());
    }

    @Test
    public void testFilterShouldReturnFalseIfLockedAndCheckerReturnsFalse() {
        ImageFilterParams imageFilterParams = createImageFilterParams(true);
        when(lockedComponentChecker.isUpgradePermitted(candidateImage, activatedParcels, BUILD_NUMBER)).thenReturn(Boolean.FALSE);

        ImageFilterResult actual = underTest.filter(new ImageFilterResult(List.of(candidateImage)), imageFilterParams);

        assertLockedCommon(actual);
    }

    @Test
    public void testFilterShouldReturnTrueIfLockedAndCheckerReturnsTrue() {
        ImageFilterParams imageFilterParams = createImageFilterParams(true);
        when(lockedComponentChecker.isUpgradePermitted(candidateImage, activatedParcels, BUILD_NUMBER)).thenReturn(Boolean.TRUE);

        ImageFilterResult actual = underTest.filter(new ImageFilterResult(List.of(candidateImage)), imageFilterParams);

        assertFalse(actual.getImages().isEmpty());
        assertTrue(actual.getReason().isEmpty());
    }

    private ImageFilterParams createImageFilterParams(boolean lockComponents) {
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage1 = com.sequenceiq.cloudbreak.cloud.model.Image.builder()
                .withOs("centos7")
                .withOsType("redhat7")
                .withPackageVersions(Map.of(ImagePackageVersion.CM_BUILD_NUMBER.getKey(), BUILD_NUMBER)).build();
        return new ImageFilterParams(null, currentImage1, null, lockComponents, false, activatedParcels, StackType.DATALAKE, null, 1L,
                new InternalUpgradeSettings(false, true, true), imageCatalogPlatform("AWS"), null, null, false);
    }

    private void assertLockedCommon(ImageFilterResult actual) {
        assertTrue(actual.getImages().isEmpty());
        assertLockedReason(actual.getReason());
        assertUpgradePermissionProviderNotInvoked();
    }

    private void assertNotLockedCommon(ImageFilterResult actual) {
        assertTrue(actual.getImages().isEmpty());
        assertNotLockedReason(actual.getReason());
        assertLockedComponentCheckerNotInvoked();
    }

    private void assertLockedReason(String reason) {
        assertEquals("There is at least one activated parcel for which we cannot find image with matching version. "
                + "Activated parcel(s): " + activatedParcels, reason);
    }

    private void assertNotLockedReason(String reason) {
        assertEquals("This service is using the latest available version.", reason);
    }

    private void assertLockedComponentCheckerNotInvoked() {
        verifyNoInteractions(lockedComponentChecker);
    }

    private void assertUpgradePermissionProviderNotInvoked() {
        verifyNoInteractions(upgradePermissionProvider);
    }
}