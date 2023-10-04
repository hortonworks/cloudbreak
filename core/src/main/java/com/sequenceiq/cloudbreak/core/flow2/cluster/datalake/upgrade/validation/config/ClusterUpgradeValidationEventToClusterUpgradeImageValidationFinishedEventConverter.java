package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.config;

import java.util.Set;

import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeImageValidationFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationEvent;
import com.sequenceiq.flow.core.PayloadConverter;

public class ClusterUpgradeValidationEventToClusterUpgradeImageValidationFinishedEventConverter
        implements PayloadConverter<ClusterUpgradeImageValidationFinishedEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return ClusterUpgradeValidationEvent.class.isAssignableFrom(sourceClass);
    }

    @Override
    public ClusterUpgradeImageValidationFinishedEvent convert(Object payload) {
        ClusterUpgradeValidationEvent sourcePayload = (ClusterUpgradeValidationEvent) payload;
        return new ClusterUpgradeImageValidationFinishedEvent(sourcePayload.selector(), sourcePayload.getResourceId(), 1L, Set.of());
    }
}
