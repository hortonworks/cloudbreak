package com.sequenceiq.cloudbreak.service.upgrade.image;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.image.CurrentImageUsageCondition;
import com.sequenceiq.common.model.OsType;

@ExtendWith(MockitoExtension.class)
class CentosToRedHatUpgradeConditionTest {

    private static final long STACK_ID = 1L;

    @InjectMocks
    private CentosToRedHatUpgradeCondition underTest;

    @Mock
    private CurrentImageUsageCondition currentImageUsageCondition;

    @Test
    void testIsCentosToRedhatUpgradeShouldReturnTrue() {
        when(currentImageUsageCondition.isCurrentOsUsedOnInstances(STACK_ID, OsType.CENTOS7.getOs())).thenReturn(true);

        assertTrue(underTest.isCentosToRedhatUpgrade(STACK_ID, createImage(OsType.RHEL8)));
    }

    @Test
    void testIsCentosToRedhatUpgradeShouldReturnFalseWhenTheTargetImageIsCentOs() {
        assertFalse(underTest.isCentosToRedhatUpgrade(STACK_ID, createImage(OsType.CENTOS7)));

        verifyNoInteractions(currentImageUsageCondition);
    }

    @Test
    void testIsCentosToRedhatUpgradeShouldReturnFalseWhenTheCurrentImageIsUsingRedHatImage() {
        assertFalse(underTest.isCentosToRedhatUpgrade(STACK_ID, createImage(OsType.CENTOS7)));
        verifyNoInteractions(currentImageUsageCondition);
    }

    private Image createImage(OsType osType) {
        return Image.builder()
                .withOs(osType.getOs())
                .withOsType(osType.getOsType())
                .build();
    }
}