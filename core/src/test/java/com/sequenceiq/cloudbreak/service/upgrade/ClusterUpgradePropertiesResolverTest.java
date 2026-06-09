package com.sequenceiq.cloudbreak.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeServiceValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationEvent;

// TODO CB-33421: Remove with ClusterUpgradePropertiesResolver once in-flight flow events always carry clusterUpgradeProperties in JSON.
@ExtendWith(MockitoExtension.class)
class ClusterUpgradePropertiesResolverTest {

    private static final long STACK_ID = 10L;

    private static final String TARGET_IMAGE_ID = "targetImageId";

    @Mock
    private ClusterUpgradePropertiesFactory clusterUpgradePropertiesFactory;

    @InjectMocks
    private ClusterUpgradePropertiesResolver underTest;

    @Test
    void testResolveReturnsExistingProperties() throws Exception {
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withRuntimeVersion("7.2.18");
        ClusterUpgradeValidationEvent event = new ClusterUpgradeValidationEvent("selector", STACK_ID, TARGET_IMAGE_ID, properties);

        assertEquals(properties, underTest.resolve(event));
    }

    @Test
    void testResolveRebuildsFromLegacyServiceValidationEvent() throws Exception {
        ClusterUpgradeServiceValidationEvent event = new ClusterUpgradeServiceValidationEvent(
                STACK_ID, TARGET_IMAGE_ID, null, true, false, "7.2.18", null, true);
        ClusterUpgradeProperties rebuilt = ClusterUpgradePropertiesTestUtils.withFlags(true, false, true);
        when(clusterUpgradePropertiesFactory.create(STACK_ID, TARGET_IMAGE_ID, true, false, true)).thenReturn(rebuilt);

        assertEquals(rebuilt, underTest.resolve(event));
        verify(clusterUpgradePropertiesFactory).create(STACK_ID, TARGET_IMAGE_ID, true, false, true);
    }

    @Test
    void testResolveRebuildsFromLegacyValidationEventWithDefaults() throws Exception {
        ClusterUpgradeValidationEvent event = new ClusterUpgradeValidationEvent("selector", STACK_ID, TARGET_IMAGE_ID, null);
        ClusterUpgradeProperties rebuilt = ClusterUpgradePropertiesTestUtils.withRuntimeVersion("7.2.18");
        when(clusterUpgradePropertiesFactory.create(STACK_ID, TARGET_IMAGE_ID, false, true, false)).thenReturn(rebuilt);

        assertEquals(rebuilt, underTest.resolve(event));
        verify(clusterUpgradePropertiesFactory).create(STACK_ID, TARGET_IMAGE_ID, false, true, false);
    }
}
