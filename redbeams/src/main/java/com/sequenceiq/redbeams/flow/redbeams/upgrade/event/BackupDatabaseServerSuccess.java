package com.sequenceiq.redbeams.flow.redbeams.upgrade.event;

import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

public class BackupDatabaseServerSuccess extends RedbeamsEvent {

    public BackupDatabaseServerSuccess(Long resourceId) {
        super(resourceId);
    }

    @Override
    public String toString() {
        return "BackupDatabaseServerSuccess{} " + super.toString();
    }

}