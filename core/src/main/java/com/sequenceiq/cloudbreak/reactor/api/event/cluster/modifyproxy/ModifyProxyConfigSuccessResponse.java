package com.sequenceiq.cloudbreak.reactor.api.event.cluster.modifyproxy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ModifyProxyConfigSuccessResponse extends StackEvent {
    @JsonCreator
    public ModifyProxyConfigSuccessResponse(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
