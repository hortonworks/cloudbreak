package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm;

import com.sequenceiq.common.api.type.Tunnel;

public class UpgradeCcmTunnelUpdateRequest extends AbstractUpgradeCcmEvent {

    public UpgradeCcmTunnelUpdateRequest(Long stackId, Long clusterId, Tunnel oldTunnel) {
        super(stackId, clusterId, oldTunnel);
    }
}
