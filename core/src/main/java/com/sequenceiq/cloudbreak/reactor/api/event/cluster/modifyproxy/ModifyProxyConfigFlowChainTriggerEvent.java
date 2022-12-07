package com.sequenceiq.cloudbreak.reactor.api.event.cluster.modifyproxy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ModifyProxyConfigFlowChainTriggerEvent extends StackEvent {

    private final String previousProxyConfigCrn;

    @JsonCreator
    public ModifyProxyConfigFlowChainTriggerEvent(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("previousProxyConfigCrn") String previousProxyConfigCrn) {
        super(stackId);
        this.previousProxyConfigCrn = previousProxyConfigCrn;
    }

    public String getPreviousProxyConfigCrn() {
        return previousProxyConfigCrn;
    }

    @Override
    public String toString() {
        return "ModifyProxyConfigFlowChainTriggerEvent{" +
                super.toString() +
                ", previousProxyConfigCrn='" + previousProxyConfigCrn + '\'' +
                '}';
    }
}
