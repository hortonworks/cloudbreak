package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.common.api.type.Tunnel;

import reactor.rx.Promise;

public class UpgradeCcmTriggerRequest extends AbstractUpgradeCcmEvent {

    public UpgradeCcmTriggerRequest(Long stackId, Long clusterId, Tunnel oldTunnel, LocalDateTime revertTime) {
        super(stackId, clusterId, oldTunnel, revertTime);
    }

    @JsonCreator
    public UpgradeCcmTriggerRequest(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("clusterId") Long clusterId,
            @JsonProperty("oldTunnel") Tunnel oldTunnel,
            @JsonProperty("revertTime") LocalDateTime revertTime,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, stackId, clusterId, oldTunnel, revertTime, accepted);
    }
}
