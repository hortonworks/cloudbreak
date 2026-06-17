package com.sequenceiq.freeipa.flow.freeipa.prepareupgrade;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeFailureCleanupComplete;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeLbDeletionSuccess;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeLbProvisionSuccess;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeMetadataCollectionSuccess;

public enum PrepareUpgradeEvent implements FlowEvent {
    PREPARE_UPGRADE_EVENT,
    PREPARE_UPGRADE_LB_CONFIGURATION_FINISHED_EVENT,
    PREPARE_UPGRADE_LB_PROVISIONED_EVENT(EventSelectorUtil.selector(PrepareUpgradeLbProvisionSuccess.class)),
    PREPARE_UPGRADE_METADATA_COLLECTED_EVENT(EventSelectorUtil.selector(PrepareUpgradeMetadataCollectionSuccess.class)),
    PREPARE_UPGRADE_LB_DELETED_EVENT(EventSelectorUtil.selector(PrepareUpgradeLbDeletionSuccess.class)),
    PREPARE_UPGRADE_LB_DB_CLEANUP_FINISHED_EVENT,
    PREPARE_UPGRADE_FINISHED_EVENT,
    PREPARE_UPGRADE_FINALIZED_EVENT,
    PREPARE_UPGRADE_FAILURE_CLEANUP_FINISHED_EVENT(EventSelectorUtil.selector(PrepareUpgradeFailureCleanupComplete.class)),
    PREPARE_UPGRADE_FAILURE_EVENT(EventSelectorUtil.selector(PrepareUpgradeFailureEvent.class)),
    PREPARE_UPGRADE_FAILURE_HANDLED_EVENT;

    private final String event;

    PrepareUpgradeEvent(String event) {
        this.event = event;
    }

    PrepareUpgradeEvent() {
        this.event = name();
    }

    @Override
    public String event() {
        return event;
    }
}
