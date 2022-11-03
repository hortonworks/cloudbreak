package com.sequenceiq.redbeams.domain.upgrade;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;

public class UpgradeDatabaseRequest {

    private TargetMajorVersion targetMajorVersion;

    public TargetMajorVersion getTargetMajorVersion() {
        return targetMajorVersion;
    }

    public void setTargetMajorVersion(TargetMajorVersion targetMajorVersion) {
        this.targetMajorVersion = targetMajorVersion;
    }

    @Override
    public String toString() {
        return "UpgradeDatabaseRequest{" +
                "targetMajorVersion=" + targetMajorVersion +
                '}';
    }
}
