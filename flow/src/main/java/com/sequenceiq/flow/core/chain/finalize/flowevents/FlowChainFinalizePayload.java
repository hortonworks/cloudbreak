package com.sequenceiq.flow.core.chain.finalize.flowevents;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.chain.finalize.config.FlowChainFinalizeEvent;

import reactor.rx.Promise;

public class FlowChainFinalizePayload implements Selectable, Acceptable {

    private Long resourceId;

    private String flowChainName;

    private final Promise<AcceptResult> accepted;

    public FlowChainFinalizePayload(String flowChainName, Long resourceId, Promise<AcceptResult> accepted) {
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
        return FlowChainFinalizeEvent.FLOWCHAIN_FINALIZE_TRIGGER_EVENT.event();
    }

    public String getFlowChainName() {
        return flowChainName;
    }
}
