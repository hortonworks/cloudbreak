package com.sequenceiq.redbeams.flow.redbeams.upgrade.event;

import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

public class UpgradeDatabaseServerSuccess extends RedbeamsEvent {
    public UpgradeDatabaseServerSuccess(Long resourceId) {
        super(resourceId);
    }

    @Override
    public String toString() {
        return "UpgradeDatabaseServerSuccess{} " + super.toString();
    }

}