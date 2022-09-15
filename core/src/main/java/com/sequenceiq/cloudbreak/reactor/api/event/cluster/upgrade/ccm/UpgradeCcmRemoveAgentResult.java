package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.common.api.type.Tunnel;

public class UpgradeCcmRemoveAgentResult extends AbstractUpgradeCcmEvent {

    private final Boolean agentDeletionSucceed;

    @JsonCreator
    public UpgradeCcmRemoveAgentResult(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("clusterId") Long clusterId,
            @JsonProperty("oldTunnel") Tunnel oldTunnel,
            @JsonProperty("revertTime") LocalDateTime revertTime,
            @JsonProperty("agentDeletionSucceed") Boolean agentDeletionSucceed) {
        super(stackId, clusterId, oldTunnel, revertTime);
        this.agentDeletionSucceed = agentDeletionSucceed;
    }

    public Boolean getAgentDeletionSucceed() {
        return agentDeletionSucceed;
    }
}
