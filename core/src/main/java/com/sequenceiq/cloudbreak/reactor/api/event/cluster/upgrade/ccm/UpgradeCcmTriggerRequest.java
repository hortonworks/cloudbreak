package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.common.api.type.Tunnel;

import reactor.rx.Promise;

public class UpgradeCcmTriggerRequest extends AbstractUpgradeCcmEvent {

    public UpgradeCcmTriggerRequest(Long stackId, Long clusterId, Tunnel oldTunnel) {
        super(stackId, clusterId, oldTunnel);
    }

    public UpgradeCcmTriggerRequest(String selector, Long stackId, Long clusterId, Tunnel oldTunnel, Promise<AcceptResult> accepted) {
        super(selector, stackId, clusterId, oldTunnel, accepted);
    }

}
