package com.sequenceiq.freeipa.flow.stack.modify.proxy.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.modify.proxy.selector.ModifyProxyConfigEvent;

public class ModifyProxyConfigTriggerEvent extends StackEvent {

    private final String operationId;

    @JsonCreator
    public ModifyProxyConfigTriggerEvent(
            @JsonProperty("resourceId") Long stackId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted,
            @JsonProperty("operationId") String operationId) {
        super(ModifyProxyConfigEvent.MODIFY_PROXY_TRIGGER_EVENT.event(), stackId, accepted);
        this.operationId = operationId;
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(ModifyProxyConfigTriggerEvent.class, other,
                event -> Objects.equals(operationId, event.operationId));
    }

    public String getOperationId() {
        return operationId;
    }

    @Override
    public String toString() {
        return "ModifyProxyConfigTriggerEvent{" +
                "operationId='" + operationId + '\'' +
                "} " + super.toString();
    }
}
