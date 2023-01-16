package com.sequenceiq.cloudbreak.reactor.api.event.cluster.modifyproxy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ModifyProxyConfigRequest extends StackEvent {

    private final String previousProxyConfigCrn;

    @JsonCreator
    public ModifyProxyConfigRequest(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("previousProxyConfigCrn") String previousProxyConfigCrn) {
        super(selector, stackId);
        this.previousProxyConfigCrn = previousProxyConfigCrn;
    }

    public String getPreviousProxyConfigCrn() {
        return previousProxyConfigCrn;
    }

    @Override
    public String toString() {
        return getClass() + "{" +
                super.toString() +
                ", previousProxyConfigCrn='" + previousProxyConfigCrn + '\'' +
                '}';
    }
}
