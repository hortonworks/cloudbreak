package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.config;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeCredentialValidationFailed;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationFailureEvent;
import com.sequenceiq.flow.core.PayloadConverter;

public class ClusterUpgradeCredentialValidationFailedToClusterUpgradeValidationFailureEvent
        implements PayloadConverter<ClusterUpgradeValidationFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return ClusterUpgradeCredentialValidationFailed.class.isAssignableFrom(sourceClass);
    }

    @Override
    public ClusterUpgradeValidationFailureEvent convert(Object payload) {
        ClusterUpgradeCredentialValidationFailed sourcePayload = (ClusterUpgradeCredentialValidationFailed) payload;
        return new ClusterUpgradeValidationFailureEvent(sourcePayload.getResourceId(), (Exception) sourcePayload.getError());
    }
}
