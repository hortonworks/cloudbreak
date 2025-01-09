package com.sequenceiq.redbeams.flow.redbeams.upgrade.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

public class ValidateUpgradeDatabaseServerCleanupSuccess extends RedbeamsEvent {

    @JsonCreator
    public ValidateUpgradeDatabaseServerCleanupSuccess(@JsonProperty("resourceId") Long resourceId) {
        super(resourceId);
    }

    @Override
    public String toString() {
        return "UpgradeDatabaseServerSuccess{} " + super.toString();
    }

}