package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmFailureEvent;

public interface UpgradeCcmFlowEvent extends FlowEvent {
    default UpgradeCcmEvent create(Long stackId, Tunnel oldTunnel) {
        return new UpgradeCcmEvent(event(), stackId, oldTunnel, null);
    }

    default UpgradeCcmEvent createBasedOn(UpgradeCcmEvent upgradeCcmEvent) {
        return new UpgradeCcmEvent(event(), upgradeCcmEvent.getResourceId(), upgradeCcmEvent.getOldTunnel(),
                upgradeCcmEvent.getRevertTime(), upgradeCcmEvent.getMinaRemoved());
    }

    default UpgradeCcmFailureEvent createBasedOn(UpgradeCcmFailureEvent upgradeCcmFailureEvent) {
        return new UpgradeCcmFailureEvent(
                event(),
                upgradeCcmFailureEvent.getResourceId(),
                upgradeCcmFailureEvent.getOldTunnel(),
                upgradeCcmFailureEvent.getFailureOrigin(),
                upgradeCcmFailureEvent.getException(),
                upgradeCcmFailureEvent.getRevertTime(),
                upgradeCcmFailureEvent.getStatusReason(),
                ERROR
        );
    }
}
