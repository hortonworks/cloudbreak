package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector;

import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmEvent;

public interface UpgradeCcmFlowEvent extends FlowEvent {
    default UpgradeCcmEvent create(Long stackId, Tunnel oldTunnel) {
        return new UpgradeCcmEvent(event(), stackId, oldTunnel);
    }

    default UpgradeCcmEvent createBasedOn(UpgradeCcmEvent upgradeCcmEvent) {
        return new UpgradeCcmEvent(event(), upgradeCcmEvent.getResourceId(), upgradeCcmEvent.getOldTunnel());
    }
}
