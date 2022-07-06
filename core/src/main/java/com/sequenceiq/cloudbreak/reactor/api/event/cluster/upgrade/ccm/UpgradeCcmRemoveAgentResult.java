package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.common.api.type.Tunnel;

public class UpgradeCcmRemoveAgentResult extends AbstractUpgradeCcmEvent {

    @JsonCreator
    public UpgradeCcmRemoveAgentResult(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("clusterId") Long clusterId,
            @JsonProperty("oldTunnel") Tunnel oldTunnel) {
        super(stackId, clusterId, oldTunnel);
    }
}
