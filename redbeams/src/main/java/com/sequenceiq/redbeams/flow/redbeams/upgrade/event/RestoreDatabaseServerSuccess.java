package com.sequenceiq.redbeams.flow.redbeams.upgrade.event;

import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

public class RestoreDatabaseServerSuccess extends RedbeamsEvent {
    public RestoreDatabaseServerSuccess(Long resourceId) {
        super(resourceId);
    }

    @Override
    public String toString() {
        return "RestoreDatabaseServerSuccess{} " + super.toString();
    }

}