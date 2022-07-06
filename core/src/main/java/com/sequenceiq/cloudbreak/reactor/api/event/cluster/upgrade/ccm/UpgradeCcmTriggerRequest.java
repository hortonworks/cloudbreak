package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.common.api.type.Tunnel;

import reactor.rx.Promise;

public class UpgradeCcmTriggerRequest extends AbstractUpgradeCcmEvent {

    public UpgradeCcmTriggerRequest(Long stackId, Long clusterId, Tunnel oldTunnel) {
        super(stackId, clusterId, oldTunnel);
    }

    @JsonCreator
    public UpgradeCcmTriggerRequest(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("clusterId") Long clusterId,
            @JsonProperty("oldTunnel") Tunnel oldTunnel,
            @JsonIgnoreDeserialization Promise<AcceptResult> accepted) {
        super(selector, stackId, clusterId, oldTunnel, accepted);
    }

}
