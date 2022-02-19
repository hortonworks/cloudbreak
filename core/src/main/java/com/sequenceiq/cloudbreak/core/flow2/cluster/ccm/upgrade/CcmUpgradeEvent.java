package com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.CcmRemoveAutoSshResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.CcmReregisterToClusterProxyResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.CcmUnregisterHostsResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.CcmUpgradeFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.CcmUpgradePreparationFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.CcmUpgradePreparationResult;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum CcmUpgradeEvent implements FlowEvent {
    CCM_UPGRADE_EVENT("CCM_UPGRADE_TRIGGER_EVENT"),
    CCM_UPGRADE_PREPARATION_FINISHED_EVENT(EventSelectorUtil.selector(CcmUpgradePreparationResult.class)),
    CCM_UPGRADE_PREPARATION_FAILED_EVENT(EventSelectorUtil.selector(CcmUpgradePreparationFailed.class)),
    CCM_UPGRADE_RE_REGISTER_FINISHED_EVENT(EventSelectorUtil.selector(CcmReregisterToClusterProxyResult.class)),
    CCM_UPGRADE_FAILED_EVENT(EventSelectorUtil.selector(CcmUpgradeFailedEvent.class)),
    CCM_UPGRADE_REMOVE_AUTOSSH_FINISHED_EVENT(EventSelectorUtil.selector(CcmRemoveAutoSshResult.class)),
    CCM_UPGRADE_UNREGISTER_HOSTS_FINISHED_EVENT(EventSelectorUtil.selector(CcmUnregisterHostsResult.class)),

    FINALIZED_EVENT("CCM_UPGRADE_FINALIZED_EVENT"),
    FAIL_HANDLED_EVENT("CCM_UPGRADE_FAIL_HANDLED_EVENT");

    private final String event;

    CcmUpgradeEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
