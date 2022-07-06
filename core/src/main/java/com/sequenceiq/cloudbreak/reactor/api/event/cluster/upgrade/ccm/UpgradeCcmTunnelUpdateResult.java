package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.common.api.type.Tunnel;

public class UpgradeCcmTunnelUpdateResult extends AbstractUpgradeCcmEvent {

    @JsonCreator
    public UpgradeCcmTunnelUpdateResult(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("clusterId") Long clusterId,
            @JsonProperty("oldTunnel") Tunnel oldTunnel) {
        super(stackId, clusterId, oldTunnel);
    }

}
