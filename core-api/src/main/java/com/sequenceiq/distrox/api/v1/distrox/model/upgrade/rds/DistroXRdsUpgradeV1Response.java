package com.sequenceiq.distrox.api.v1.distrox.model.upgrade.rds;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.flow.api.model.FlowIdentifier;

public class DistroXRdsUpgradeV1Response {

    private TargetMajorVersion targetVersion;

    private FlowIdentifier flowIdentifier;

    public DistroXRdsUpgradeV1Response() {
    }

    public DistroXRdsUpgradeV1Response(FlowIdentifier flowIdentifier, TargetMajorVersion targetVersion) {
        this.flowIdentifier = flowIdentifier;
        this.targetVersion = targetVersion;
    }

    public TargetMajorVersion getTargetVersion() {
        return targetVersion;
    }

    public void setTargetVersion(TargetMajorVersion targetVersion) {
        this.targetVersion = targetVersion;
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    public void setFlowIdentifier(FlowIdentifier flowIdentifier) {
        this.flowIdentifier = flowIdentifier;
    }

    @Override
    public String toString() {
        return "DistroXRdsUpgradeV1Response{" +
                ", targetVersion=" + targetVersion +
                ", flowIdentifier=" + flowIdentifier +
                '}';
    }
}
