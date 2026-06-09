package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.config;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationHandlerSelectors.VALIDATE_DISK_SPACE_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeImageValidationFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationEvent;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesResolver;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesTestUtils;

@ExtendWith(MockitoExtension.class)
public class ClusterUpgradeValidationEventToClusterUpgradeImageValidationFinishedEventConverterTest {
    private static final long RESOURCE_ID = 25L;

    @Mock
    private ClusterUpgradePropertiesResolver clusterUpgradePropertiesResolver;

    @InjectMocks
    private ClusterUpgradeValidationEventToClusterUpgradeImageValidationFinishedEventConverter underTest;

    @BeforeEach
    void setUp() {
        when(clusterUpgradePropertiesResolver.resolveUnchecked(any())).thenAnswer(invocation ->
                ((ClusterUpgradeValidationEvent) invocation.getArgument(0)).getClusterUpgradeProperties());
    }

    @Test
    public void testConvertShouldCreateAClusterUpgradeDiskSpaceValidationEvent() {
        ClusterUpgradeProperties clusterUpgradeProperties = ClusterUpgradePropertiesTestUtils.withRuntimeVersion("7.2.18");
        ClusterUpgradeValidationEvent sourceEvent = new ClusterUpgradeValidationEvent(VALIDATE_DISK_SPACE_EVENT.event(), RESOURCE_ID,
                clusterUpgradeProperties.getTargetImageId(), clusterUpgradeProperties);
        ClusterUpgradeImageValidationFinishedEvent actual = underTest.convert(sourceEvent);

        assertEquals(RESOURCE_ID, actual.getResourceId());
        assertEquals(clusterUpgradeProperties, actual.getClusterUpgradeProperties());
        assertEquals(clusterUpgradeProperties.getTargetImageId(), actual.getClusterUpgradeProperties().getTargetImageId());
        assertEquals(1L, actual.getRequiredFreeSpace());
        assertTrue(actual.getWarningMessages().isEmpty());
    }
}
