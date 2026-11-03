package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeDatalakeReachabilityValidationFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeFreeIpaStatusValidationFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeS3guardValidationFinishedEvent;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesTestUtils;
import com.sequenceiq.common.model.OsType;

class ClusterUpgradeFreeIpaStatusValidationFinishedToClusterUpgradeDatalakeReachabilityValidationFinishedEventConverterTest {

    private static final Long STACK_ID = 1L;

    private final ClusterUpgradeFreeIpaStatusValidationFinishedToClusterUpgradeDatalakeReachabilityValidationFinishedEventConverter underTest =
            new ClusterUpgradeFreeIpaStatusValidationFinishedToClusterUpgradeDatalakeReachabilityValidationFinishedEventConverter();

    @Test
    void canConvertFreeIpaStatusValidationFinishedEventOnly() {
        assertTrue(underTest.canConvert(ClusterUpgradeFreeIpaStatusValidationFinishedEvent.class));
        assertFalse(underTest.canConvert(ClusterUpgradeS3guardValidationFinishedEvent.class));
    }

    @Test
    void convertKeepsTheStackImageAndUpgradeProperties() {
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withTargetProducts(
                "7.3.1", "base-image", OsType.RHEL8, "x86_64", null, Set.of(), null);
        ClusterUpgradeFreeIpaStatusValidationFinishedEvent source =
                new ClusterUpgradeFreeIpaStatusValidationFinishedEvent(STACK_ID, properties.getTargetImageId(), properties);

        ClusterUpgradeDatalakeReachabilityValidationFinishedEvent result = underTest.convert(source);

        assertEquals(STACK_ID, result.getResourceId());
        assertEquals(properties.getTargetImageId(), result.getImageId());
        assertSame(properties, result.getClusterUpgradeProperties());
    }
}
