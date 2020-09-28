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

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradePermissionProvider;
import com.sequenceiq.cloudbreak.service.upgrade.image.locked.LockedComponentChecker;

@ExtendWith(MockitoExtension.class)
class CmAndStackVersionFilterTest {
    private static final String V_7_0_3 = "7.0.3";

    private static final String STACK_PACKAGE_KEY = "stack";

    private static final String CM_PACKAGE_KEY = "cm";

    private static final String CDH_BUILD_NUMBER_KEY = "cdh-build-number";

    private static final String CM_BUILD_NUMBER_KEY = "cm-build-number";

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
        when(upgradePermissionProvider.permitCmAndStackUpgrade(currentImage, candidateImage, STACK_PACKAGE_KEY, CDH_BUILD_NUMBER_KEY)).thenReturn(Boolean.FALSE);
        when(upgradePermissionProvider.permitCmAndStackUpgrade(currentImage, candidateImage, CM_PACKAGE_KEY, CM_BUILD_NUMBER_KEY)).thenReturn(Boolean.TRUE);

        Predicate<Image> predicate = underTest.filterCmAndStackVersion(currentImage, false, activatedParcels, reason);
        boolean result = predicate.test(candidateImage);

        assertFalse(result);
        assertNotLockedCommon();
    }

    @Test
    public void testFilterShouldReturnFalseWhenNotLockedAndCMPermitcheckIsFalse() {
        lenient().when(upgradePermissionProvider.permitCmAndStackUpgrade(currentImage, candidateImage, STACK_PACKAGE_KEY, CDH_BUILD_NUMBER_KEY))
                .thenReturn(Boolean.TRUE);
        when(upgradePermissionProvider.permitCmAndStackUpgrade(currentImage, candidateImage, CM_PACKAGE_KEY, CM_BUILD_NUMBER_KEY)).thenReturn(Boolean.FALSE);

        Predicate<Image> predicate = underTest.filterCmAndStackVersion(currentImage, false, activatedParcels, reason);
        boolean result = predicate.test(candidateImage);

        assertFalse(result);
        assertNotLockedCommon();
    }

    @Test
    public void testFilterShouldReturnFalseWhenNotLockedAndCMAndStackPermitcheckIsFalse() {
        lenient().when(upgradePermissionProvider.permitCmAndStackUpgrade(currentImage, candidateImage, STACK_PACKAGE_KEY, CDH_BUILD_NUMBER_KEY))
                .thenReturn(Boolean.FALSE);
        when(upgradePermissionProvider.permitCmAndStackUpgrade(currentImage, candidateImage, CM_PACKAGE_KEY, CM_BUILD_NUMBER_KEY)).thenReturn(Boolean.FALSE);

        Predicate<Image> predicate = underTest.filterCmAndStackVersion(currentImage, false, activatedParcels, reason);
        boolean result = predicate.test(candidateImage);

        assertFalse(result);
        assertNotLockedCommon();
    }

    @Test
    public void testFilterShouldReturnTrueWhenNotLockedAndCMAndStackPermitcheckIsTrue() {
        when(upgradePermissionProvider.permitCmAndStackUpgrade(currentImage, candidateImage, STACK_PACKAGE_KEY, CDH_BUILD_NUMBER_KEY)).thenReturn(Boolean.TRUE);
        when(upgradePermissionProvider.permitCmAndStackUpgrade(currentImage, candidateImage, CM_PACKAGE_KEY, CM_BUILD_NUMBER_KEY)).thenReturn(Boolean.TRUE);

        Predicate<Image> predicate = underTest.filterCmAndStackVersion(currentImage, false, activatedParcels, reason);
        boolean result = predicate.test(candidateImage);

        assertTrue(result);
        assertNotLockedCommon();
    }

    @Test
    public void testFilterShouldReturnFalseIfLockedAndCheckerReturnsFalse() {
        when(lockedComponentChecker.isUpgradePermitted(currentImage, candidateImage, activatedParcels)).thenReturn(Boolean.FALSE);

        Predicate<Image> predicate = underTest.filterCmAndStackVersion(currentImage, true, activatedParcels, reason);
        boolean result = predicate.test(candidateImage);

        assertFalse(result);
        assertLockedCommon();
    }

    @Test
    public void testFilterShouldReturnTrueIfLockedAndCheckerReturnsTrue() {
        when(lockedComponentChecker.isUpgradePermitted(currentImage, candidateImage, activatedParcels)).thenReturn(Boolean.TRUE);

        Predicate<Image> predicate = underTest.filterCmAndStackVersion(currentImage, true, activatedParcels, reason);
        boolean result = predicate.test(candidateImage);

        assertTrue(result);
        assertLockedCommon();
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