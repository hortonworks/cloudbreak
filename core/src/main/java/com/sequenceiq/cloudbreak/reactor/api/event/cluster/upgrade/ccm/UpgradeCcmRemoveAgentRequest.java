package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm;

import com.sequenceiq.common.api.type.Tunnel;

public class UpgradeCcmRemoveAgentRequest extends AbstractUpgradeCcmEvent {

    public UpgradeCcmRemoveAgentRequest(Long stackId, Long clusterId, Tunnel oldTunnel) {
        super(stackId, clusterId, oldTunnel);
    }
}
