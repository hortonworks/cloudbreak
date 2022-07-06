package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.recovery.bringup;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class DatalakeRecoverySetupNewInstancesSuccess extends StackEvent {

    @JsonCreator
    public DatalakeRecoverySetupNewInstancesSuccess(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
