package com.sequenceiq.distrox.api.v1.distrox.model.upgrade.rds;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.api.model.RdsUpgradeResponseType;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.flow.api.model.FlowIdentifier;

public class DistroXRdsUpgradeV1Response {

    private RdsUpgradeResponseType responseType;

    private TargetMajorVersion targetVersion;

    private String reason;

    private FlowIdentifier flowIdentifier;

    public DistroXRdsUpgradeV1Response() {
    }

    public DistroXRdsUpgradeV1Response(RdsUpgradeResponseType responseType, FlowIdentifier flowIdentifier, String reason, TargetMajorVersion targetVersion) {
        this.responseType = responseType;
        this.flowIdentifier = flowIdentifier;
        this.reason = reason;
        this.targetVersion = targetVersion;
    }

    public TargetMajorVersion getTargetVersion() {
        return targetVersion;
    }

    public void setTargetVersion(TargetMajorVersion targetVersion) {
        this.targetVersion = targetVersion;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void appendReason(String reason) {
        if (StringUtils.isNotEmpty(this.reason)) {
            this.reason += " " + reason;
        } else {
            setReason(reason);
        }
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
                "responseType=" + responseType +
                ", targetVersion=" + targetVersion +
                ", reason='" + reason + '\'' +
                ", flowIdentifier=" + flowIdentifier +
                '}';
    }
}
