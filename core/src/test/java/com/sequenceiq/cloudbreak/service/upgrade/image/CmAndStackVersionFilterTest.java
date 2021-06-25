package com.sequenceiq.cloudbreak.service.upgrade.image;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.function.Predicate;

import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradePermissionProvider;
import com.sequenceiq.cloudbreak.service.upgrade.image.locked.LockedComponentChecker;

@ExtendWith(MockitoExtension.class)
class CmAndStackVersionFilterTest {

    private static final String V_7_0_3 = "7.0.3";

    @Mock
    private LockedComponentChecker lockedComponentChecker;

    @Mock
    private UpgradePermissionProvider upgradePermissionProvider;

    @InjectMocks
    private CmAndStackVersionFilter underTest;

    private final Image currentImage = mock(Image.class);

    private final Image candidateImage = mock(Image.class);

    private final Map<String, String> activatedParcels = Map.of("stack", V_7_0_3);

    private Mutable<String> reason;

    @BeforeEach
    public void init() {
        reason = new MutableObject<>();
    }

    @Test
    public void testFilterShouldReturnFalseWhenNotLockedAndStackPermitcheckIsFalse() {
        ImageFilterParams imageFilterParams = createImageFilterParams(false);
        when(upgradePermissionProvider.permitStackUpgrade(imageFilterParams, candidateImage))
                .thenReturn(Boolean.FALSE);
        when(upgradePermissionProvider.permitCmUpgrade(imageFilterParams, candidateImage))
                .thenReturn(Boolean.TRUE);

        Predicate<Image> predicate = underTest.filterCmAndStackVersion(imageFilterParams, reason);
        boolean result = predicate.test(candidateImage);

        assertFalse(result);
        assertNotLockedCommon();
    }

    @Test
    public void testFilterShouldReturnFalseWhenNotLockedAndCMPermitcheckIsFalse() {
        ImageFilterParams imageFilterParams = createImageFilterParams(false);
        lenient().when(upgradePermissionProvider.permitStackUpgrade(imageFilterParams, candidateImage))
                .thenReturn(Boolean.TRUE);
        when(upgradePermissionProvider.permitCmUpgrade(imageFilterParams, candidateImage))
                .thenReturn(Boolean.FALSE);

        Predicate<Image> predicate = underTest.filterCmAndStackVersion(imageFilterParams, reason);
        boolean result = predicate.test(candidateImage);

        assertFalse(result);
        assertNotLockedCommon();
    }

    @Test
    public void testFilterShouldReturnFalseWhenNotLockedAndCMAndStackPermitcheckIsFalse() {
        ImageFilterParams imageFilterParams = createImageFilterParams(false);
        lenient().when(upgradePermissionProvider.permitStackUpgrade(imageFilterParams, candidateImage))
                .thenReturn(Boolean.FALSE);
        when(upgradePermissionProvider.permitCmUpgrade(imageFilterParams, candidateImage))
                .thenReturn(Boolean.FALSE);

        Predicate<Image> predicate = underTest.filterCmAndStackVersion(imageFilterParams, reason);
        boolean result = predicate.test(candidateImage);

        assertFalse(result);
        assertNotLockedCommon();
    }

    @Test
    public void testFilterShouldReturnTrueWhenNotLockedAndCMAndStackPermitcheckIsTrue() {
        ImageFilterParams imageFilterParams = createImageFilterParams(false);
        when(upgradePermissionProvider.permitStackUpgrade(imageFilterParams, candidateImage))
                .thenReturn(Boolean.TRUE);
        when(upgradePermissionProvider.permitCmUpgrade(imageFilterParams, candidateImage))
                .thenReturn(Boolean.TRUE);

        Predicate<Image> predicate = underTest.filterCmAndStackVersion(imageFilterParams, reason);
        boolean result = predicate.test(candidateImage);

        assertTrue(result);
        assertNotLockedCommon();
    }

    @Test
    public void testFilterShouldReturnFalseIfLockedAndCheckerReturnsFalse() {
        ImageFilterParams imageFilterParams = createImageFilterParams(true);
        when(lockedComponentChecker.isUpgradePermitted(currentImage, candidateImage, activatedParcels)).thenReturn(Boolean.FALSE);

        Predicate<Image> predicate = underTest.filterCmAndStackVersion(imageFilterParams, reason);
        boolean result = predicate.test(candidateImage);

        assertFalse(result);
        assertLockedCommon();
    }

    @Test
    public void testFilterShouldReturnTrueIfLockedAndCheckerReturnsTrue() {
        ImageFilterParams imageFilterParams = createImageFilterParams(true);
        when(lockedComponentChecker.isUpgradePermitted(currentImage, candidateImage, activatedParcels)).thenReturn(Boolean.TRUE);

        Predicate<Image> predicate = underTest.filterCmAndStackVersion(imageFilterParams, reason);
        boolean result = predicate.test(candidateImage);

        assertTrue(result);
        assertLockedCommon();
    }

    private ImageFilterParams createImageFilterParams(boolean lockComponents) {
        return new ImageFilterParams(currentImage, lockComponents, activatedParcels, StackType.DATALAKE, null, 1L);
    }

    private void assertLockedCommon() {
        assertLockedReason();
        assertUpgradePermissionProviderNotInvoked();
    }

    private void assertNotLockedCommon() {
        assertNotLockedReason();
        assertLockedComponentCheckerNotInvoked();
    }

    private void assertLockedReason() {
        assertEquals("There is at least one activated parcel for which we cannot find image with matching version. "
                + "Activated parcel(s): " + activatedParcels, reason.getValue());
    }

    private void assertNotLockedReason() {
        assertEquals("There is no proper Cloudera Manager or CDP version to upgrade.", reason.getValue());
    }

    private void assertLockedComponentCheckerNotInvoked() {
        verifyNoInteractions(lockedComponentChecker);
    }

    private void assertUpgradePermissionProviderNotInvoked() {
        verifyNoInteractions(upgradePermissionProvider);
    }
}
