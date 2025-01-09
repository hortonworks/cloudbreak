package com.sequenceiq.redbeams.flow.redbeams.upgrade.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsValidateUpgradeEvent;

public class RedbeamsStartValidateUpgradeCleanupRequest extends RedbeamsEvent {

    @JsonCreator
    public RedbeamsStartValidateUpgradeCleanupRequest(
            @JsonProperty("resourceId") Long resourceId) {
        super(RedbeamsValidateUpgradeEvent.REDBEAMS_START_VALIDATE_UPGRADE_EVENT.selector(), resourceId);
    }

    @Override
    public String toString() {
        return "RedbeamsStartValidateUpgradeCleanupRequest{} " + super.toString();
    }

    @Override
    public boolean equalsEvent(RedbeamsEvent other) {
        return isClassAndEqualsEvent(RedbeamsStartValidateUpgradeCleanupRequest.class, other);
    }
}