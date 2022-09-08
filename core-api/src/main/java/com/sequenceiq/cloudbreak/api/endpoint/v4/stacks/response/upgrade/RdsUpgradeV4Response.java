package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.flow.api.model.FlowIdentifier;

public class RdsUpgradeV4Response {

    private TargetMajorVersion targetVersion;

    private FlowIdentifier flowIdentifier;

    public RdsUpgradeV4Response() {
    }

    public RdsUpgradeV4Response(FlowIdentifier flowIdentifier, TargetMajorVersion targetVersion) {
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
        return "RdsUpgradeV4Response{" +
                ", targetVersion=" + targetVersion +
                ", flowIdentifier=" + flowIdentifier +
                '}';
    }
}
