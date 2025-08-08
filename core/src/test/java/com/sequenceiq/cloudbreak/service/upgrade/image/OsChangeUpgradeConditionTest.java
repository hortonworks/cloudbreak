package com.sequenceiq.cloudbreak.service.upgrade.image;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.image.CurrentImageUsageCondition;
import com.sequenceiq.common.model.OsType;

@ExtendWith(MockitoExtension.class)
class OsChangeUpgradeConditionTest {

    private static final long STACK_ID = 1L;

    @InjectMocks
    private OsChangeUpgradeCondition underTest;

    @Mock
    private CurrentImageUsageCondition currentImageUsageCondition;

    @Test
    void testIsNextMajorReleaseBetweenCentosAndRhel8() {
        when(currentImageUsageCondition.getOSUsedByInstances(STACK_ID)).thenReturn(Set.of(OsType.CENTOS7));

        assertTrue(underTest.isNextMajorOsImage(STACK_ID, createImage(OsType.RHEL8)));
    }

    @Test
    void testIsNextMajorReleaseBetweenRhel8AndRhel9() {
        when(currentImageUsageCondition.getOSUsedByInstances(STACK_ID)).thenReturn(Set.of(OsType.RHEL8));

        assertTrue(underTest.isNextMajorOsImage(STACK_ID, createImage(OsType.RHEL9)));
    }

    @Test
    void testIsNextMajorReleaseBetweenCentosAndCentos() {
        assertFalse(underTest.isNextMajorOsImage(STACK_ID, createImage(OsType.CENTOS7)));

        verifyNoInteractions(currentImageUsageCondition);
    }

    @Test
    void testIsNextMajorReleaseBetweenRhel8AndRhel8() {
        when(currentImageUsageCondition.getOSUsedByInstances(STACK_ID)).thenReturn(Set.of(OsType.RHEL8));

        assertFalse(underTest.isNextMajorOsImage(STACK_ID, createImage(OsType.RHEL8)));
    }

    @Test
    void testIsNextMajorReleaseBetweenRhel9AndRhel9() {
        when(currentImageUsageCondition.getOSUsedByInstances(STACK_ID)).thenReturn(Set.of(OsType.RHEL9));

        assertFalse(underTest.isNextMajorOsImage(STACK_ID, createImage(OsType.RHEL9)));
    }

    private Image createImage(OsType osType) {
        return Image.builder()
                .withOs(osType.getOs())
                .withOsType(osType.getOsType())
                .build();
    }
}