package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.config;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationHandlerSelectors.VALIDATE_DISK_SPACE_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeImageValidationFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationEvent;

@ExtendWith(MockitoExtension.class)
public class ClusterUpgradeValidationEventToClusterUpgradeImageValidationFinishedEventConverterTest {
    private static final long RESOURCE_ID = 25L;

    @InjectMocks
    private ClusterUpgradeValidationEventToClusterUpgradeImageValidationFinishedEventConverter underTest;

    @Test
    public void testConvertShouldCreateAClusterUpgradeDiskSpaceValidationEvent() {
        ClusterUpgradeValidationEvent sourceEvent = new ClusterUpgradeValidationEvent(VALIDATE_DISK_SPACE_EVENT.event(), RESOURCE_ID, "image-id");
        ClusterUpgradeImageValidationFinishedEvent actual = underTest.convert(sourceEvent);

        assertEquals(RESOURCE_ID, actual.getResourceId());
        assertEquals(1L, actual.getRequiredFreeSpace());
        assertTrue(actual.getWarningMessages().isEmpty());
    }
}
