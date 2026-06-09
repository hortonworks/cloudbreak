package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.config;

import java.util.Set;

import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeDiskSpaceValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeImageValidationFinishedEvent;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesResolver;
import com.sequenceiq.flow.core.PayloadConverter;

public class ClusterUpgradeDiskSpaceValidationEventToClusterUpgradeImageValidationFinishedEventConverter
        implements PayloadConverter<ClusterUpgradeImageValidationFinishedEvent> {

    // TODO CB-33421: Remove resolver once in-flight flow events always carry clusterUpgradeProperties in JSON.
    private final ClusterUpgradePropertiesResolver clusterUpgradePropertiesResolver;

    public ClusterUpgradeDiskSpaceValidationEventToClusterUpgradeImageValidationFinishedEventConverter(
            ClusterUpgradePropertiesResolver clusterUpgradePropertiesResolver) {
        this.clusterUpgradePropertiesResolver = clusterUpgradePropertiesResolver;
    }

    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return ClusterUpgradeDiskSpaceValidationEvent.class.isAssignableFrom(sourceClass);
    }

    @Override
    public ClusterUpgradeImageValidationFinishedEvent convert(Object payload) {
        ClusterUpgradeDiskSpaceValidationEvent sourcePayload = (ClusterUpgradeDiskSpaceValidationEvent) payload;
        ClusterUpgradeProperties clusterUpgradeProperties = clusterUpgradePropertiesResolver.resolveUnchecked(sourcePayload);
        return new ClusterUpgradeImageValidationFinishedEvent(sourcePayload.selector(), sourcePayload.getResourceId(),
                clusterUpgradeProperties.getTargetImageId(), clusterUpgradeProperties, sourcePayload.getRequiredFreeSpace(), Set.of());
    }
}
