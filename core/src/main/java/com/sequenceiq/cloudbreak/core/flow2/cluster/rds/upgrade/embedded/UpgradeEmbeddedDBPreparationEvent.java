package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.embedded;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.embeddeddb.UpgradeEmbeddedDbPrepareResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsDataBackupResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsDataRestoreResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsStartServicesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsStopServicesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsUpgradeDatabaseServerResult;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum UpgradeEmbeddedDBPreparationEvent implements FlowEvent {
    UPGRADE_EMBEDDEDDB_PREPARATION_EVENT("UPGRADE_EMBEDDEDDB_PREPARATION_TRIGGER_EVENT"),
    UPGRADE_EMBEDDEDDB_PREPARATION_FINISHED_EVENT(EventSelectorUtil.selector(UpgradeEmbeddedDbPrepareResult.class)),
    UPGRADE_EMBEDDEDDB_PREPARATION_FAILED_EVENT(EventSelectorUtil.selector(UpgradeRdsFailedEvent.class)),
    FINALIZED_EVENT("UPGRADE_RDS_FINALIZED_EVENT"),
    FAIL_HANDLED_EVENT("UPGRADE_RDS_FAIL_HANDLED_EVENT");

    private final String event;

    UpgradeEmbeddedDBPreparationEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
