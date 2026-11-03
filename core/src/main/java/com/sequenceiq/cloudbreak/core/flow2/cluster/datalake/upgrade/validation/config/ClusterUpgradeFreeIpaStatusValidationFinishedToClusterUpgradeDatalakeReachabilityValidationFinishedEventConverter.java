package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.config;

import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeDatalakeReachabilityValidationFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeFreeIpaStatusValidationFinishedEvent;
import com.sequenceiq.flow.core.PayloadConverter;

/**
 * Before the Data Lake reachability validation was introduced the service validation followed the FreeIPA status validation,
 * so a flow persisted before restarts from the FreeIPA status validation state and enters the service validation state with the
 * FreeIPA status validation result as its payload.
 */
public class ClusterUpgradeFreeIpaStatusValidationFinishedToClusterUpgradeDatalakeReachabilityValidationFinishedEventConverter
        implements PayloadConverter<ClusterUpgradeDatalakeReachabilityValidationFinishedEvent> {

    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return ClusterUpgradeFreeIpaStatusValidationFinishedEvent.class.isAssignableFrom(sourceClass);
    }

    @Override
    public ClusterUpgradeDatalakeReachabilityValidationFinishedEvent convert(Object payload) {
        ClusterUpgradeFreeIpaStatusValidationFinishedEvent sourcePayload = (ClusterUpgradeFreeIpaStatusValidationFinishedEvent) payload;
        return new ClusterUpgradeDatalakeReachabilityValidationFinishedEvent(sourcePayload.getResourceId(), sourcePayload.getImageId(),
                sourcePayload.getClusterUpgradeProperties());
    }
}
