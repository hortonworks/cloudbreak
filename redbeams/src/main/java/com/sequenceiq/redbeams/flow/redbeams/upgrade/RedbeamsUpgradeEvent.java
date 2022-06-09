package com.sequenceiq.redbeams.flow.redbeams.upgrade;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.BackupDatabaseServerSuccess;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.RedbeamsUpgradeFailedEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.RestoreDatabaseServerSuccess;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.UpgradeDatabaseServerSuccess;

public enum RedbeamsUpgradeEvent implements FlowEvent {

    REDBEAMS_START_UPGRADE_EVENT("REDBEAMS_START_UPGRADE_EVENT"),
    BACKUP_DATABASE_SERVER_FINISHED_EVENT(EventSelectorUtil.selector(BackupDatabaseServerSuccess.class)),
    UPGRADE_DATABASE_SERVER_FINISHED_EVENT(EventSelectorUtil.selector(UpgradeDatabaseServerSuccess.class)),
    RESTORE_DATABASE_SERVER_FINISHED_EVENT(EventSelectorUtil.selector(RestoreDatabaseServerSuccess.class)),
    REDBEAMS_UPGRADE_FAILED_EVENT(EventSelectorUtil.selector(RedbeamsUpgradeFailedEvent.class)),
    REDBEAMS_UPGRADE_FAILURE_HANDLED_EVENT("REDBEAMS_UPGRADE_FAILURE_HANDLED_EVENT"),
    REDBEAMS_UPGRADE_FINISHED_EVENT("REDBEAMS_UPGRADE_FINISHED_EVENT");

    private final String event;

    RedbeamsUpgradeEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }

}