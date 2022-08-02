package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.embedded;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.embeddeddb.UpgradeEmbeddedDBPreparationFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.embeddeddb.UpgradeEmbeddedDBPreparationResult;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum UpgradeEmbeddedDBPreparationEvent implements FlowEvent {

    UPGRADE_EMBEDDEDDB_PREPARATION_EVENT("UPGRADE_EMBEDDEDDB_PREPARATION_TRIGGER_EVENT"),
    UPGRADE_EMBEDDEDDB_PREPARATION_FINISHED_EVENT(EventSelectorUtil.selector(UpgradeEmbeddedDBPreparationResult.class)),
    UPGRADE_EMBEDDEDDB_PREPARATION_FAILED_EVENT(EventSelectorUtil.selector(UpgradeEmbeddedDBPreparationFailedEvent.class)),
    FINALIZED_EVENT("UPGRADE_EMBEDDEDDB_PREPARATION_FINALIZED_EVENT"),
    FAIL_HANDLED_EVENT("UPGRADE_EMBEDDEDDB_PREPARATION__FAIL_HANDLED_EVENT");

    private final String event;

    UpgradeEmbeddedDBPreparationEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
