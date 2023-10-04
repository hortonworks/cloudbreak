package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.config;

import java.util.Set;

import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeDiskSpaceValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeImageValidationFinishedEvent;
import com.sequenceiq.flow.core.PayloadConverter;

public class ClusterUpgradeDiskSpaceValidationEventToClusterUpgradeImageValidationFinishedEventConverter
        implements PayloadConverter<ClusterUpgradeImageValidationFinishedEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return ClusterUpgradeDiskSpaceValidationEvent.class.isAssignableFrom(sourceClass);
    }

    @Override
    public ClusterUpgradeImageValidationFinishedEvent convert(Object payload) {
        ClusterUpgradeDiskSpaceValidationEvent sourcePayload = (ClusterUpgradeDiskSpaceValidationEvent) payload;
        return new ClusterUpgradeImageValidationFinishedEvent(sourcePayload.selector(), sourcePayload.getResourceId(),
                sourcePayload.getRequiredFreeSpace(), Set.of());
    }
}
