package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeFreeIpaStatusValidationEvent extends StackEvent {

    public ClusterUpgradeFreeIpaStatusValidationEvent(Long resourceId) {
        super(ClusterUpgradeValidationHandlerSelectors.VALIDATE_FREEIPA_STATUS_EVENT.name(), resourceId);
    }
}
