package com.sequenceiq.redbeams.flow.redbeams.upgrade.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

public class BackupDatabaseServerSuccess extends RedbeamsEvent {

    @JsonCreator
    public BackupDatabaseServerSuccess(@JsonProperty("resourceId") Long resourceId) {
        super(resourceId);
    }

    @Override
    public String toString() {
        return "BackupDatabaseServerSuccess{} " + super.toString();
    }

}
