package com.sequenceiq.freeipa.flow.freeipa.downscale.event.stophealthagent;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class StopHealthAgentRequest extends StackEvent {

    private final List<String> fqdns;

    @JsonCreator
    public StopHealthAgentRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("fqdns") List<String> fqdns) {
        super(stackId);
        this.fqdns = fqdns;
    }

    public List<String> getFqdns() {
        return fqdns;
    }

    @Override
    public String toString() {
        return "StopHealthAgentRequest{" +
                "fqdns=" + fqdns +
                "} " + super.toString();
    }
}
