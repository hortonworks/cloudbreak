package com.sequenceiq.redbeams.flow.redbeams.upgrade.event;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsUpgradeEvent;

public class RedbeamsStartUpgradeRequest extends RedbeamsEvent {

    private final TargetMajorVersion targetMajorVersion;

    public RedbeamsStartUpgradeRequest(Long resourceId, TargetMajorVersion targetMajorVersion) {
        super(RedbeamsUpgradeEvent.REDBEAMS_START_UPGRADE_EVENT.selector(), resourceId);
        this.targetMajorVersion = targetMajorVersion;
    }

    public TargetMajorVersion getTargetMajorVersion() {
        return targetMajorVersion;
    }

    @Override
    public String toString() {
        return "RedbeamsStartUpgradeRequest{" +
                "targetMajorVersion=" + targetMajorVersion +
                "} " + super.toString();
    }

}
