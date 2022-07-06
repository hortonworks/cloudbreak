package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeFreeIpaStatusValidationEvent extends StackEvent {

    @JsonCreator
    public ClusterUpgradeFreeIpaStatusValidationEvent(
            @JsonProperty("resourceId") Long resourceId) {
        super(ClusterUpgradeValidationHandlerSelectors.VALIDATE_FREEIPA_STATUS_EVENT.name(), resourceId);
    }
}
