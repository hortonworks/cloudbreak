package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.config;

import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeDiskSpaceValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationEvent;
import com.sequenceiq.flow.core.PayloadConverter;

public class ClusterUpgradeValidationEventToClusterUpgradeDiskSpaceValidationEventConverter
        implements PayloadConverter<ClusterUpgradeDiskSpaceValidationEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return ClusterUpgradeValidationEvent.class.isAssignableFrom(sourceClass);
    }

    @Override
    public ClusterUpgradeDiskSpaceValidationEvent convert(Object payload) {
        ClusterUpgradeValidationEvent sourcePayload = (ClusterUpgradeValidationEvent) payload;
        return new ClusterUpgradeDiskSpaceValidationEvent(sourcePayload.selector(), sourcePayload.getResourceId(), 1L);
    }
}
