package com.sequenceiq.redbeams.flow.redbeams.upgrade;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.RedbeamsValidateUpgradeFailedEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.ValidateUpgradeDatabaseServerSuccess;

public enum RedbeamsValidateUpgradeEvent implements FlowEvent {

    REDBEAMS_START_VALIDATE_UPGRADE_EVENT("REDBEAMS_START_VALIDATE_UPGRADE_EVENT"),
    VALIDATE_UPGRADE_DATABASE_SERVER_FINISHED_EVENT(EventSelectorUtil.selector(ValidateUpgradeDatabaseServerSuccess.class)),
    REDBEAMS_VALIDATE_UPGRADE_FAILED_EVENT(EventSelectorUtil.selector(RedbeamsValidateUpgradeFailedEvent.class)),
    REDBEAMS_VALIDATE_UPGRADE_FAILURE_HANDLED_EVENT("REDBEAMS_VALIDATE_UPGRADE_FAILURE_HANDLED_EVENT"),
    REDBEAMS_VALIDATE_UPGRADE_FINISHED_EVENT("REDBEAMS_VALIDATE_UPGRADE_FINISHED_EVENT");

    private final String event;

    RedbeamsValidateUpgradeEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }

}