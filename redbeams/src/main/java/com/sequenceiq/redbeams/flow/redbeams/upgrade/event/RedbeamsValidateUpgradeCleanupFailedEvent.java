package com.sequenceiq.redbeams.flow.redbeams.upgrade.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFailureEvent;

public class RedbeamsValidateUpgradeCleanupFailedEvent extends RedbeamsFailureEvent {

    @JsonCreator
    public RedbeamsValidateUpgradeCleanupFailedEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("exception") Exception exception) {

        super(resourceId, exception);
    }

}