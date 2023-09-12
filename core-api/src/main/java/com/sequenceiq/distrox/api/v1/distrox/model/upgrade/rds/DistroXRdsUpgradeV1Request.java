package com.sequenceiq.distrox.api.v1.distrox.model.upgrade.rds;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;

public class DistroXRdsUpgradeV1Request {

    private TargetMajorVersion targetVersion;

    private Boolean forced;

    public TargetMajorVersion getTargetVersion() {
        return targetVersion;
    }

    public void setTargetVersion(TargetMajorVersion targetVersion) {
        this.targetVersion = targetVersion;
    }

    public Boolean getForced() {
        return forced;
    }

    public void setForced(Boolean forced) {
        this.forced = forced;
    }

    @Override
    public String toString() {
        return "DistroXRdsUpgradeV1Request{" +
                "targetVersion=" + targetVersion +
                ", forced=" + forced +
                '}';
    }
}
