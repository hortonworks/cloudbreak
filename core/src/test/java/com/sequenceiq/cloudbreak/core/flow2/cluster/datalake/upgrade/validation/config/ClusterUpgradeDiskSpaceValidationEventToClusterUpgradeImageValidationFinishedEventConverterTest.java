package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.config;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationHandlerSelectors.VALIDATE_DISK_SPACE_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeDiskSpaceValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeImageValidationFinishedEvent;

@ExtendWith(MockitoExtension.class)
class ClusterUpgradeDiskSpaceValidationEventToClusterUpgradeImageValidationFinishedEventConverterTest {
    private static final long RESOURCE_ID = 25L;

    @InjectMocks
    private ClusterUpgradeDiskSpaceValidationEventToClusterUpgradeImageValidationFinishedEventConverter underTest;

    @Test
    public void testConvertShouldCreateAClusterUpgradeDiskSpaceValidationEvent() {
        ClusterUpgradeDiskSpaceValidationEvent sourceEvent = new ClusterUpgradeDiskSpaceValidationEvent(VALIDATE_DISK_SPACE_EVENT.event(), RESOURCE_ID, 10L);
        ClusterUpgradeImageValidationFinishedEvent actual = underTest.convert(sourceEvent);

        assertEquals(RESOURCE_ID, actual.getResourceId());
        assertEquals(10L, actual.getRequiredFreeSpace());
        assertTrue(actual.getWarningMessages().isEmpty());
    }
}
