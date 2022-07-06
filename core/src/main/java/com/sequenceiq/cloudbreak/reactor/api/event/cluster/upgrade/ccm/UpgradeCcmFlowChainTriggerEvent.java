package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.common.api.type.Tunnel;

import reactor.rx.Promise;

public class UpgradeCcmFlowChainTriggerEvent extends StackEvent {

    private final Tunnel oldTunnel;

    private final Long clusterId;

    public UpgradeCcmFlowChainTriggerEvent(String selector, Long stackId, Long clusterId, Tunnel oldTunnel) {
        super(selector, stackId);
        this.clusterId = clusterId;
        this.oldTunnel = oldTunnel;
    }

    @JsonCreator
    public UpgradeCcmFlowChainTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("clusterId") Long clusterId,
            @JsonProperty("oldTunnel") Tunnel oldTunnel,
            @JsonIgnoreDeserialization Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
        this.clusterId = clusterId;
        this.oldTunnel = oldTunnel;
    }

    public Tunnel getOldTunnel() {
        return oldTunnel;
    }

    public Long getClusterId() {
        return clusterId;
    }

    @Override
    public String toString() {
        return "UpgradeCcmFlowChainTriggerEvent{" +
                " oldTunnel=" + oldTunnel +
                ",clusterId=" + clusterId +
                "} " + super.toString();
    }
}
