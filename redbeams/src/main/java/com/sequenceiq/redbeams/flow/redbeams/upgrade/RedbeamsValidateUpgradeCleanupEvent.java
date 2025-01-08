package com.sequenceiq.redbeams.flow.redbeams.upgrade;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.RedbeamsValidateUpgradeCleanupFailedEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.ValidateUpgradeDatabaseServerCleanupSuccess;

public enum RedbeamsValidateUpgradeCleanupEvent implements FlowEvent {

    REDBEAMS_START_VALIDATE_UPGRADE_CLEANUP_EVENT("REDBEAMS_START_VALIDATE_UPGRADE_CLEANUP_EVENT"),
    VALIDATE_UPGRADE_CLEANUP_DATABASE_SERVER_FINISHED_EVENT(EventSelectorUtil.selector(ValidateUpgradeDatabaseServerCleanupSuccess.class)),
    REDBEAMS_VALIDATE_UPGRADE_CLEANUP_FAILED_EVENT(EventSelectorUtil.selector(RedbeamsValidateUpgradeCleanupFailedEvent.class)),
    REDBEAMS_VALIDATE_UPGRADE_CLEANUP_FAILURE_HANDLED_EVENT("REDBEAMS_VALIDATE_UPGRADE_CLEANUP_FAILURE_HANDLED_EVENT"),
    REDBEAMS_VALIDATE_UPGRADE_CLEANUP_FINISHED_EVENT("REDBEAMS_VALIDATE_UPGRADE_CLEANUP_FINISHED_EVENT");

    private final String event;

    RedbeamsValidateUpgradeCleanupEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }

}