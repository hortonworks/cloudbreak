package com.sequenceiq.flow.core.chain.init.flowevents;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.flow.core.chain.init.config.FlowChainInitEvent;

public class FlowChainInitPayload implements Selectable, Acceptable {

    private final Long resourceId;

    private final String flowChainName;

    private final Promise<AcceptResult> accepted;

    @JsonCreator
    public FlowChainInitPayload(
            @JsonProperty("flowChainName") String flowChainName,
            @JsonProperty("resourceId") Long resourceId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {

        this.flowChainName = flowChainName;
        this.resourceId = resourceId;
        this.accepted = accepted;
    }

    @Override
    public Long getResourceId() {
        return resourceId;
    }

    @Override
    public Promise<AcceptResult> accepted() {
        return accepted;
    }

    @Override
    public String selector() {
        return FlowChainInitEvent.FLOWCHAIN_INIT_TRIGGER_EVENT.event();
    }

    public String getFlowChainName() {
        return flowChainName;
    }
}
