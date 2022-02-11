package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmCheckPrerequisitesRequest;

public enum UpgradeCcmHandlerSelector implements FlowEvent {

    // TODO handler trigger events
    UPGRADE_CCM_CHECK_PREREQUISITES_EVENT(EventSelectorUtil.selector(UpgradeCcmCheckPrerequisitesRequest.class));

    private final String event;

    UpgradeCcmHandlerSelector(String event) {
        this.event = event;
    }

    UpgradeCcmHandlerSelector() {
        this.event = name();
    }

    @Override
    public String event() {
        return event;
    }

}
