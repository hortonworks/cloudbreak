package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.action;

import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmEvent;

public abstract class AbstractUpgradeCcmEventAction extends AbstractUpgradeCcmAction<UpgradeCcmEvent> {

    protected AbstractUpgradeCcmEventAction() {
        super(UpgradeCcmEvent.class);
    }
}
