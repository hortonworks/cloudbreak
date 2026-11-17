package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.config;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeImageValidationFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationEvent;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesResolver;
import com.sequenceiq.flow.core.PayloadConverter;

public class ClusterUpgradeValidationEventToClusterUpgradeImageValidationFinishedEventConverter
        implements PayloadConverter<ClusterUpgradeImageValidationFinishedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeValidationEventToClusterUpgradeImageValidationFinishedEventConverter.class);

    // TODO CB-33421: Remove this converter once in-flight flow events always carry requiredFreeSpace in JSON.
    private static final long UNKNOWN_REQUIRED_FREE_SPACE_KB = 1L;

    // TODO CB-33421: Remove resolver once in-flight flow events always carry clusterUpgradeProperties in JSON.
    private final ClusterUpgradePropertiesResolver clusterUpgradePropertiesResolver;

    public ClusterUpgradeValidationEventToClusterUpgradeImageValidationFinishedEventConverter(
            ClusterUpgradePropertiesResolver clusterUpgradePropertiesResolver) {
        this.clusterUpgradePropertiesResolver = clusterUpgradePropertiesResolver;
    }

    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return ClusterUpgradeValidationEvent.class.equals(sourceClass);
    }

    @Override
    public ClusterUpgradeImageValidationFinishedEvent convert(Object payload) {
        ClusterUpgradeValidationEvent sourcePayload = (ClusterUpgradeValidationEvent) payload;
        LOGGER.warn("The required free space is unknown for the legacy {} payload of stack {}, using {} KB, so the disk space validation is a no-op.",
                sourcePayload.getClass().getSimpleName(), sourcePayload.getResourceId(), UNKNOWN_REQUIRED_FREE_SPACE_KB);
        ClusterUpgradeProperties clusterUpgradeProperties = clusterUpgradePropertiesResolver.resolve(sourcePayload);
        return new ClusterUpgradeImageValidationFinishedEvent(sourcePayload.selector(), sourcePayload.getResourceId(),
                clusterUpgradeProperties.getTargetImageId(), clusterUpgradeProperties, UNKNOWN_REQUIRED_FREE_SPACE_KB, Set.of());
    }
}
