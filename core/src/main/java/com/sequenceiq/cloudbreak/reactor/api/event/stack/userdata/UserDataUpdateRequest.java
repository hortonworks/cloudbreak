package com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.common.api.type.Tunnel;

public class UserDataUpdateRequest extends StackEvent {

    private final Tunnel oldTunnel;

    public UserDataUpdateRequest(Long stackId, Tunnel oldTunnel) {
        super(stackId);
        this.oldTunnel = oldTunnel;
    }

    @JsonCreator
    public UserDataUpdateRequest(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("oldTunnel") Tunnel oldTunnel) {
        super(selector, stackId);
        this.oldTunnel = oldTunnel;
    }

    public Tunnel getOldTunnel() {
        return oldTunnel;
    }

    @Override
    public String toString() {
        return "UserDataUpdateRequest{" +
                " oldTunnel=" + oldTunnel +
                "} " + super.toString();
    }
}
