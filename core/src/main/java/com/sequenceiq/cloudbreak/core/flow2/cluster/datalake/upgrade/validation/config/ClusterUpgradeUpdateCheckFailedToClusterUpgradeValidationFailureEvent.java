package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.config;

import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeUpddateCheckFailed;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationFailureEvent;
import com.sequenceiq.flow.core.PayloadConverter;

public class ClusterUpgradeUpdateCheckFailedToClusterUpgradeValidationFailureEvent
        implements PayloadConverter<ClusterUpgradeValidationFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return ClusterUpgradeUpddateCheckFailed.class.isAssignableFrom(sourceClass);
    }

    @Override
    public ClusterUpgradeValidationFailureEvent convert(Object payload) {
        ClusterUpgradeUpddateCheckFailed sourcePayload = (ClusterUpgradeUpddateCheckFailed) payload;
        return new ClusterUpgradeValidationFailureEvent(sourcePayload.getResourceId(), (Exception) sourcePayload.getError());
    }
}
