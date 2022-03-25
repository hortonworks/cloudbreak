package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmEvent;

public interface UpgradeCcmFlowEvent extends FlowEvent {
    default UpgradeCcmEvent create(Long stackId) {
        return new UpgradeCcmEvent(event(), stackId);
    }

    default UpgradeCcmEvent createBasedOn(UpgradeCcmEvent upgradeCcmEvent) {
        UpgradeCcmEvent result = new UpgradeCcmEvent(event(), upgradeCcmEvent.getResourceId());
        upgradeCcmEvent.getCcmConnectivityParameters().ifPresent(cp -> result.setCcmConnectivityParameters(cp));
        upgradeCcmEvent.getOldTunnel().ifPresent(ot -> result.setOldTunnel(ot));

        return result;
    }
}
