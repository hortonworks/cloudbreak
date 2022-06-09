package com.sequenceiq.redbeams.flow.redbeams.upgrade.event;

import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFailureEvent;

public class RedbeamsUpgradeFailedEvent extends RedbeamsFailureEvent {

    public RedbeamsUpgradeFailedEvent(Long resourceId, Exception exception) {
        super(resourceId, exception);
    }

}