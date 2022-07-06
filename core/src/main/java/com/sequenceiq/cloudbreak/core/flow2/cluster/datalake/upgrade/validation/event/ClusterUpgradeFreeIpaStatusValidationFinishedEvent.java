package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeFreeIpaStatusValidationFinishedEvent extends StackEvent {

    @JsonCreator
    public ClusterUpgradeFreeIpaStatusValidationFinishedEvent(
            @JsonProperty("resourceId") Long resourceId) {
        super(ClusterUpgradeValidationStateSelectors.FINISH_CLUSTER_UPGRADE_FREEIPA_STATUS_VALIDATION_EVENT.name(), resourceId);
    }
}
