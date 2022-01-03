package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradePermissionProvider;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;
import com.sequenceiq.cloudbreak.service.upgrade.image.locked.LockedComponentChecker;

@ExtendWith(MockitoExtension.class)
class CmAndStackVersionUpgradeImageFilterTest {

    private static final String V_7_0_3 = "7.0.3";

    @Mock
    private LockedComponentChecker lockedComponentChecker;

    @Mock
    private UpgradePermissionProvider upgradePermissionProvider;

    @InjectMocks
    private CmAndStackVersionUpgradeImageFilter underTest;

    private final Image currentImage = mock(Image.class);

    private final Image candidateImage = mock(Image.class);

    private final Map<String, String> activatedParcels = Map.of("stack", V_7_0_3);

    @Test
    public void testFilterShouldReturnErrorMessageWhenNotLockedAndStackPermitCheckIsFalse() {
        ImageFilterParams imageFilterParams = createImageFilterParams(false);
        when(upgradePermissionProvider.permitStackUpgrade(imageFilterParams, candidateImage)).thenReturn(Boolean.FALSE);
        when(upgradePermissionProvider.permitCmUpgrade(imageFilterParams, candidateImage)).thenReturn(Boolean.TRUE);

        ImageFilterResult actual = underTest.filter(new ImageFilterResult(List.of(candidateImage)), imageFilterParams);

        assertNotLockedCommon(actual);
    }

    @Test
    public void testFilterShouldReturnErrorMessageWhenNotLockedAndCMPermitcheckIsFalse() {
        ImageFilterParams imageFilterParams = createImageFilterParams(false);
        lenient().when(upgradePermissionProvider.permitStackUpgrade(imageFilterParams, candidateImage)).thenReturn(Boolean.TRUE);
        when(upgradePermissionProvider.permitCmUpgrade(imageFilterParams, candidateImage)).thenReturn(Boolean.FALSE);

        ImageFilterResult actual = underTest.filter(new ImageFilterResult(List.of(candidateImage)), imageFilterParams);

        assertNotLockedCommon(actual);
    }

    @Test
    public void testFilterShouldReturnFalseWhenNotLockedAndCMAndStackPermitcheckIsFalse() {
        ImageFilterParams imageFilterParams = createImageFilterParams(false);
        lenient().when(upgradePermissionProvider.permitStackUpgrade(imageFilterParams, candidateImage)).thenReturn(Boolean.FALSE);
        when(upgradePermissionProvider.permitCmUpgrade(imageFilterParams, candidateImage)).thenReturn(Boolean.FALSE);

        ImageFilterResult actual = underTest.filter(new ImageFilterResult(List.of(candidateImage)), imageFilterParams);

        assertNotLockedCommon(actual);
    }

    @Test
    public void testFilterShouldReturnTrueWhenNotLockedAndCMAndStackPermitcheckIsTrue() {
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
        when(lockedComponentChecker.isUpgradePermitted(currentImage, candidateImage, activatedParcels)).thenReturn(Boolean.FALSE);

        ImageFilterResult actual = underTest.filter(new ImageFilterResult(List.of(candidateImage)), imageFilterParams);

        assertLockedCommon(actual);
    }

    @Test
    public void testFilterShouldReturnTrueIfLockedAndCheckerReturnsTrue() {
        ImageFilterParams imageFilterParams = createImageFilterParams(true);
        when(lockedComponentChecker.isUpgradePermitted(currentImage, candidateImage, activatedParcels)).thenReturn(Boolean.TRUE);

        ImageFilterResult actual = underTest.filter(new ImageFilterResult(List.of(candidateImage)), imageFilterParams);

        assertFalse(actual.getImages().isEmpty());
        assertTrue(actual.getReason().isEmpty());
    }

    private ImageFilterParams createImageFilterParams(boolean lockComponents) {
        return new ImageFilterParams(currentImage, lockComponents, activatedParcels, StackType.DATALAKE, null, 1L, new InternalUpgradeSettings(false, true,
                true), "AWS");
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
        assertEquals("There is no proper Cloudera Manager or CDP version to upgrade.", reason);
    }

    private void assertLockedComponentCheckerNotInvoked() {
        verifyNoInteractions(lockedComponentChecker);
    }

    private void assertUpgradePermissionProviderNotInvoked() {
        verifyNoInteractions(upgradePermissionProvider);
    }
}
